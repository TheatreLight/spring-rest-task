package com.hotel.bookingservice.service;

import com.hotel.bookingservice.dto.*;
import com.hotel.bookingservice.entity.*;
import com.hotel.bookingservice.exception.*;
import com.hotel.bookingservice.mapper.BookingMapper;
import com.hotel.bookingservice.repository.BookingRepository;
import com.hotel.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelServiceCaller hotelServiceCaller;
    private final BookingMapper bookingMapper;

    @Transactional(readOnly = true)
    public List<BookingDto> getUserBookings(Long userId) {
        log.debug("Fetching bookings for user: {}", userId);
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookingMapper.toDtoList(bookings);
    }

    @Transactional(readOnly = true)
    public Page<BookingDto> getUserBookings(Long userId, Pageable pageable) {
        log.debug("Fetching bookings for user: {} with pagination", userId);
        return bookingRepository.findUserBookingsOrderByCreatedAtDesc(userId, pageable)
                .map(bookingMapper::toDto);
    }

    @Transactional(readOnly = true)
    public BookingDto getBookingById(Long bookingId, Long userId) {
        log.debug("Fetching booking {} for user {}", bookingId, userId);
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return bookingMapper.toDto(booking);
    }

    @Transactional
    public BookingDto createBooking(Long userId, CreateBookingRequest request) {
        String requestId = request.getRequestId() != null ? request.getRequestId() : UUID.randomUUID().toString();
        log.info("Creating booking for user {} with requestId: {}", userId, requestId);

        // Idempotency check
        BookingDto existingBooking = checkIdempotency(requestId);
        if (existingBooking != null) {
            return existingBooking;
        }

        // Validate dates
        validateDates(request.getStartDate(), request.getEndDate());

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Determine room to book
        Long roomId = determineRoomId(request);
        Long hotelId = request.getHotelId();

        // Get room info if hotelId not provided
        if (hotelId == null) {
            try {
                RoomDto room = hotelServiceCaller.getRoomById(roomId);
                hotelId = room.getHotelId();
            } catch (Exception e) {
                log.warn("Could not fetch room info: {}", e.getMessage());
            }
        }

        // Step 1: Create booking in PENDING status (local transaction)
        Booking booking = Booking.builder()
                .user(user)
                .roomId(roomId)
                .hotelId(hotelId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(BookingStatus.PENDING)
                .requestId(requestId)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking created in PENDING status with id: {} requestId: {}", booking.getId(), requestId);

        // Step 2: Confirm availability with Hotel Service (with retry and circuit breaker)
        try {
            AvailabilityResponse response = confirmRoomAvailability(booking, requestId);

            if (!response.isConfirmed()) {
                throw new BookingException("Room is not available for the requested dates");
            }

            // Step 3: Update booking status to CONFIRMED
            booking.confirm();
            booking = bookingRepository.save(booking);

            // Step 4: Confirm booking to increment times_booked
            confirmBookingWithHotelService(roomId, requestId);

            log.info("Booking {} confirmed successfully for room {}", booking.getId(), roomId);

        } catch (Exception e) {
            log.error("Failed to confirm availability for booking {}: {}", booking.getId(), e.getMessage());

            // Compensation: Cancel booking and release lock
            compensateBooking(booking, requestId);

            throw new BookingException("Failed to create booking: " + e.getMessage(), e);
        }

        return bookingMapper.toDto(booking);
    }

    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        log.info("Cancelling booking {} for user {}", bookingId, userId);

        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.info("Booking {} is already cancelled", bookingId);
            return;
        }

        String requestId = booking.getRequestId();

        // Release the room lock in Hotel Service
        try {
            ReleaseRoomRequest releaseRequest = ReleaseRoomRequest.builder()
                    .requestId(requestId)
                    .bookingId(bookingId)
                    .build();
            hotelServiceCaller.releaseRoom(booking.getRoomId(), releaseRequest);
            log.info("Room {} released for booking {}", booking.getRoomId(), bookingId);
        } catch (Exception e) {
            log.warn("Failed to release room for booking {}: {}", bookingId, e.getMessage());
            // Continue with cancellation even if release fails
        }

        booking.cancel();
        bookingRepository.save(booking);
        log.info("Booking {} cancelled successfully", bookingId);
    }

    private BookingDto checkIdempotency(String requestId) {
        return bookingRepository.findByRequestId(requestId)
                .map(existing -> {
                    log.info("Request {} already processed, returning existing booking {}",
                            requestId, existing.getId());
                    return bookingMapper.toDto(existing);
                })
                .orElse(null);
    }

    private Long determineRoomId(CreateBookingRequest request) {
        if (Boolean.TRUE.equals(request.getAutoSelect())) {
            log.debug("Auto-selecting room for hotel {} and dates {} - {}",
                    request.getHotelId(), request.getStartDate(), request.getEndDate());

            List<RoomDto> recommendedRooms = hotelServiceCaller.getRecommendedRooms(
                    request.getHotelId(),
                    request.getStartDate(),
                    request.getEndDate());

            if (recommendedRooms.isEmpty()) {
                throw new BookingException("No available rooms found for the selected dates");
            }

            // Return the room with lowest times_booked (first in the sorted list)
            return recommendedRooms.get(0).getId();
        }

        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("Room ID is required when autoSelect is false");
        }

        return request.getRoomId();
    }

    private AvailabilityResponse confirmRoomAvailability(Booking booking, String requestId) {
        log.debug("Confirming availability for room {} with requestId {}", booking.getRoomId(), requestId);

        ConfirmAvailabilityRequest confirmRequest = ConfirmAvailabilityRequest.builder()
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .requestId(requestId)
                .bookingId(booking.getId())
                .build();

        return hotelServiceCaller.confirmAvailability(booking.getRoomId(), confirmRequest);
    }

    private void confirmBookingWithHotelService(Long roomId, String requestId) {
        try {
            hotelServiceCaller.confirmBooking(roomId, requestId);
        } catch (Exception e) {
            log.warn("Failed to confirm booking with hotel service: {}", e.getMessage());
            // Non-critical - booking is already confirmed
        }
    }

    @Transactional
    protected void compensateBooking(Booking booking, String requestId) {
        log.info("Compensating booking {} with requestId {}", booking.getId(), requestId);

        // Update booking status to CANCELLED
        booking.cancel();
        bookingRepository.save(booking);

        // Release the room lock
        try {
            ReleaseRoomRequest releaseRequest = ReleaseRoomRequest.builder()
                    .requestId(requestId)
                    .bookingId(booking.getId())
                    .build();
            hotelServiceCaller.releaseRoom(booking.getRoomId(), releaseRequest);
            log.info("Room {} released as part of compensation", booking.getRoomId());
        } catch (Exception e) {
            log.error("Failed to release room during compensation: {}", e.getMessage());
            // Log for manual intervention but don't throw
        }

        log.info("Compensation completed for booking {}", booking.getId());
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
    }
}

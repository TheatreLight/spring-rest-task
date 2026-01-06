package com.hotel.hotelservice.service;

import com.hotel.hotelservice.dto.*;
import com.hotel.hotelservice.entity.*;
import com.hotel.hotelservice.exception.*;
import com.hotel.hotelservice.mapper.HotelMapper;
import com.hotel.hotelservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomLockRepository roomLockRepository;
    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    public List<RoomDto> getAllRooms() {
        log.debug("Fetching all available rooms");
        List<Room> rooms = roomRepository.findByAvailableTrue();
        return hotelMapper.toRoomDtoList(rooms);
    }

    public List<RoomDto> getRoomsByHotel(Long hotelId) {
        log.debug("Fetching rooms for hotel: {}", hotelId);
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        return hotelMapper.toRoomDtoList(rooms);
    }

    public RoomDto getRoomById(Long id) {
        log.debug("Fetching room by id: {}", id);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
        return hotelMapper.toRoomDto(room);
    }

    public List<RoomDto> getRecommendedRooms() {
        log.debug("Fetching recommended rooms sorted by times_booked");
        List<Room> rooms = roomRepository.findAvailableRoomsSortedByTimesBooked();
        return hotelMapper.toRoomDtoList(rooms);
    }

    public List<RoomDto> getRecommendedRoomsForDates(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching recommended rooms for dates: {} - {}", startDate, endDate);
        validateDates(startDate, endDate);
        List<Room> rooms = roomRepository.findAvailableRoomsForDates(startDate, endDate);
        return hotelMapper.toRoomDtoList(rooms);
    }

    public List<RoomDto> getRecommendedRoomsForDatesByHotel(Long hotelId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching recommended rooms for hotel {} and dates: {} - {}", hotelId, startDate, endDate);
        validateDates(startDate, endDate);
        List<Room> rooms = roomRepository.findAvailableRoomsForDatesByHotel(hotelId, startDate, endDate);
        return hotelMapper.toRoomDtoList(rooms);
    }

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        log.info("Creating new room in hotel: {}", request.getHotelId());

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", request.getHotelId()));

        roomRepository.findByHotelIdAndNumber(request.getHotelId(), request.getNumber())
                .ifPresent(r -> {
                    throw new DuplicateResourceException(
                            "Room with number '" + request.getNumber() + "' already exists in this hotel");
                });

        Room room = hotelMapper.toEntity(request);
        room.setHotel(hotel);
        room = roomRepository.save(room);

        log.info("Room created successfully with id: {}", room.getId());
        return hotelMapper.toRoomDto(room);
    }

    @Transactional
    public RoomDto updateRoom(Long id, CreateRoomRequest request) {
        log.info("Updating room with id: {}", id);

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));

        if (!room.getHotel().getId().equals(request.getHotelId())) {
            Hotel newHotel = hotelRepository.findById(request.getHotelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel", request.getHotelId()));
            room.setHotel(newHotel);
        }

        roomRepository.findByHotelIdAndNumber(request.getHotelId(), request.getNumber())
                .filter(r -> !r.getId().equals(id))
                .ifPresent(r -> {
                    throw new DuplicateResourceException(
                            "Room with number '" + request.getNumber() + "' already exists in this hotel");
                });

        room.setNumber(request.getNumber());
        room.setAvailable(request.getAvailable() != null ? request.getAvailable() : room.getAvailable());
        room = roomRepository.save(room);

        log.info("Room updated successfully: {}", id);
        return hotelMapper.toRoomDto(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        log.info("Deleting room with id: {}", id);

        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room", id);
        }

        roomLockRepository.findByRoomId(id).forEach(lock -> roomLockRepository.delete(lock));
        roomRepository.deleteById(id);
        log.info("Room deleted successfully: {}", id);
    }

    @Transactional
    public AvailabilityResponse confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        String requestId = request.getRequestId();
        log.info("Confirming availability for room {} with requestId: {}", roomId, requestId);

        // Idempotency check
        Optional<RoomLock> existingLock = roomLockRepository.findByRequestId(requestId);
        if (existingLock.isPresent()) {
            log.info("Request {} already processed, returning existing result", requestId);
            RoomLock lock = existingLock.get();
            return AvailabilityResponse.builder()
                    .roomId(roomId)
                    .requestId(requestId)
                    .startDate(lock.getStartDate())
                    .endDate(lock.getEndDate())
                    .confirmed(true)
                    .message("Already confirmed (idempotent response)")
                    .build();
        }

        validateDates(request.getStartDate(), request.getEndDate());

        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        if (!room.getAvailable()) {
            throw new RoomNotAvailableException("Room is not operationally available");
        }

        // Check for overlapping locks
        boolean hasOverlap = roomLockRepository.existsOverlappingLock(
                roomId, request.getStartDate(), request.getEndDate());

        if (hasOverlap) {
            log.warn("Room {} is already booked for the requested dates", roomId);
            throw new RoomNotAvailableException(roomId);
        }

        // Create lock
        RoomLock lock = RoomLock.builder()
                .room(room)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .requestId(requestId)
                .bookingId(request.getBookingId())
                .confirmed(false)
                .build();

        roomLockRepository.save(lock);

        log.info("Room {} locked successfully for dates {} - {} with requestId {}",
                roomId, request.getStartDate(), request.getEndDate(), requestId);

        return AvailabilityResponse.builder()
                .roomId(roomId)
                .requestId(requestId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .confirmed(true)
                .message("Room availability confirmed and locked")
                .build();
    }

    @Transactional
    public void confirmBooking(Long roomId, String requestId) {
        log.info("Confirming booking for room {} with requestId: {}", roomId, requestId);

        RoomLock lock = roomLockRepository.findByRoomIdAndRequestId(roomId, requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Lock not found for requestId: " + requestId));

        lock.setConfirmed(true);
        roomLockRepository.save(lock);

        Room room = lock.getRoom();
        room.incrementTimesBooked();
        roomRepository.save(room);

        log.info("Booking confirmed for room {} with requestId: {}", roomId, requestId);
    }

    @Transactional
    public void releaseRoom(Long roomId, ReleaseRoomRequest request) {
        String requestId = request.getRequestId();
        log.info("Releasing room {} with requestId: {}", roomId, requestId);

        Optional<RoomLock> lockOpt = roomLockRepository.findByRoomIdAndRequestId(roomId, requestId);

        if (lockOpt.isEmpty()) {
            log.info("No lock found for room {} with requestId {} - possibly already released", roomId, requestId);
            return;
        }

        RoomLock lock = lockOpt.get();

        if (lock.getConfirmed()) {
            log.warn("Attempting to release a confirmed lock for room {} - this should decrement times_booked", roomId);
            Room room = lock.getRoom();
            if (room.getTimesBooked() > 0) {
                room.setTimesBooked(room.getTimesBooked() - 1);
                roomRepository.save(room);
            }
        }

        roomLockRepository.delete(lock);
        log.info("Room {} released successfully for requestId: {}", roomId, requestId);
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

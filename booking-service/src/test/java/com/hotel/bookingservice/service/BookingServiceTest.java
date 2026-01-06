package com.hotel.bookingservice.service;

import com.hotel.bookingservice.dto.*;
import com.hotel.bookingservice.entity.*;
import com.hotel.bookingservice.exception.*;
import com.hotel.bookingservice.mapper.BookingMapper;
import com.hotel.bookingservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HotelServiceCaller hotelServiceCaller;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Booking testBooking;
    private BookingDto testBookingDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        testBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .requestId("test-request-id")
                .build();

        testBookingDto = BookingDto.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .requestId("test-request-id")
                .build();
    }

    @Test
    void getUserBookings_ShouldReturnUserBookings() {
        // Given
        when(bookingRepository.findByUserId(1L)).thenReturn(Arrays.asList(testBooking));
        when(bookingMapper.toDtoList(anyList())).thenReturn(Arrays.asList(testBookingDto));

        // When
        List<BookingDto> result = bookingService.getUserBookings(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void getBookingById_ShouldReturnBooking() {
        // Given
        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testBooking));
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingDto);

        // When
        BookingDto result = bookingService.getBookingById(1L, 1L);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRoomId()).isEqualTo(1L);
    }

    @Test
    void getBookingById_WithNonExistentBooking_ShouldThrowException() {
        // Given
        when(bookingRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookingService.getBookingById(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createBooking_WithIdempotentRequest_ShouldReturnExisting() {
        // Given
        String requestId = "existing-request-id";
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .requestId(requestId)
                .build();

        when(bookingRepository.findByRequestId(requestId)).thenReturn(Optional.of(testBooking));
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingDto);

        // When
        BookingDto result = bookingService.createBooking(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository, never()).findById(anyLong());
        verify(hotelServiceCaller, never()).confirmAvailability(anyLong(), any());
    }

    @Test
    void createBooking_WithInvalidDates_ShouldThrowException() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(3)) // End before start
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date cannot be before start date");
    }

    @Test
    void createBooking_WithPastStartDate_ShouldThrowException() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().minusDays(1)) // Past date
                .endDate(LocalDate.now().plusDays(3))
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be in the past");
    }

    @Test
    void createBooking_WithAutoSelect_ShouldSelectRecommendedRoom() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(true)
                .build();

        RoomDto recommendedRoom = RoomDto.builder()
                .id(2L)
                .hotelId(1L)
                .number("102")
                .timesBooked(0)
                .build();

        AvailabilityResponse availabilityResponse = AvailabilityResponse.builder()
                .roomId(2L)
                .confirmed(true)
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hotelServiceCaller.getRecommendedRooms(eq(1L), any(), any()))
                .thenReturn(Arrays.asList(recommendedRoom));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        when(hotelServiceCaller.confirmAvailability(eq(2L), any())).thenReturn(availabilityResponse);
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(testBookingDto);

        // When
        BookingDto result = bookingService.createBooking(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(hotelServiceCaller).getRecommendedRooms(eq(1L), any(), any());
    }

    @Test
    void createBooking_WithAutoSelectAndNoRooms_ShouldThrowException() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(true)
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hotelServiceCaller.getRecommendedRooms(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("No available rooms");
    }

    @Test
    void cancelBooking_ShouldCancelAndRelease() {
        // Given
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        bookingService.cancelBooking(1L, 1L);

        // Then
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(hotelServiceCaller).releaseRoom(eq(1L), any(ReleaseRoomRequest.class));
        verify(bookingRepository).save(testBooking);
    }

    @Test
    void cancelBooking_WithAlreadyCancelled_ShouldNotCallRelease() {
        // Given
        testBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testBooking));

        // When
        bookingService.cancelBooking(1L, 1L);

        // Then
        verify(hotelServiceCaller, never()).releaseRoom(anyLong(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_WithNonExistentBooking_ShouldThrowException() {
        // Given
        when(bookingRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookingService.cancelBooking(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ========== NEW TESTS FOR MISSING SCENARIOS ==========

    @Test
    void createBooking_WhenHotelServiceTimeout_ShouldCompensateAndThrow() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        // Simulate timeout by throwing HotelServiceException
        when(hotelServiceCaller.confirmAvailability(eq(1L), any()))
                .thenThrow(new HotelServiceException("Connection timeout", new RuntimeException()));

        // When/Then
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Failed to create booking");

        // Verify compensation was attempted
        verify(hotelServiceCaller).releaseRoom(eq(1L), any(ReleaseRoomRequest.class));
        // Verify booking was saved with CANCELLED status (compensation)
        verify(bookingRepository, atLeast(2)).save(any(Booking.class));
    }

    @Test
    void createBooking_WhenRoomNotAvailable_ShouldCompensateAndThrow() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        AvailabilityResponse notAvailableResponse = AvailabilityResponse.builder()
                .roomId(1L)
                .confirmed(false)
                .message("Room already booked for these dates")
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        when(hotelServiceCaller.confirmAvailability(eq(1L), any())).thenReturn(notAvailableResponse);

        // When/Then
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Room is not available");

        // Verify compensation was attempted
        verify(hotelServiceCaller).releaseRoom(eq(1L), any(ReleaseRoomRequest.class));
    }

    @Test
    void createBooking_WhenCircuitBreakerOpen_ShouldThrowHotelServiceException() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        // Simulate circuit breaker open
        when(hotelServiceCaller.confirmAvailability(eq(1L), any()))
                .thenThrow(new HotelServiceException("Hotel service is unavailable. Please try again later.",
                        new RuntimeException("CircuitBreaker 'hotelService' is OPEN")));

        // When/Then
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Hotel service is unavailable");
    }

    @Test
    void createBooking_SuccessfulFlow_ShouldConfirmBooking() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        AvailabilityResponse availabilityResponse = AvailabilityResponse.builder()
                .roomId(1L)
                .confirmed(true)
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            if (b.getId() == null) b.setId(1L);
            return b;
        });
        when(hotelServiceCaller.confirmAvailability(eq(1L), any())).thenReturn(availabilityResponse);
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(testBookingDto);

        // When
        BookingDto result = bookingService.createBooking(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(hotelServiceCaller).confirmAvailability(eq(1L), any());
        verify(hotelServiceCaller).confirmBooking(eq(1L), any());
        verify(hotelServiceCaller, never()).releaseRoom(anyLong(), any()); // No compensation
    }

    @Test
    void createBooking_WhenConfirmBookingFails_ShouldStillSucceed() {
        // Given - confirmBooking is non-critical, booking should still succeed
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        AvailabilityResponse availabilityResponse = AvailabilityResponse.builder()
                .roomId(1L)
                .confirmed(true)
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            if (b.getId() == null) b.setId(1L);
            return b;
        });
        when(hotelServiceCaller.confirmAvailability(eq(1L), any())).thenReturn(availabilityResponse);
        doThrow(new RuntimeException("Network error")).when(hotelServiceCaller).confirmBooking(anyLong(), any());
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(testBookingDto);

        // When
        BookingDto result = bookingService.createBooking(1L, request);

        // Then - booking should still succeed
        assertThat(result).isNotNull();
        verify(hotelServiceCaller).confirmBooking(eq(1L), any()); // Was attempted
    }

    @Test
    void cancelBooking_WhenReleaseRoomFails_ShouldStillCancelBooking() {
        // Given
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Network error")).when(hotelServiceCaller).releaseRoom(anyLong(), any());

        // When
        bookingService.cancelBooking(1L, 1L);

        // Then - booking should still be cancelled locally
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    void createBooking_WithoutRoomId_AndAutoSelectFalse_ShouldThrowException() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(false)
                // roomId is null
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room ID is required when autoSelect is false");
    }

    @Test
    void createBooking_WithNullDates_ShouldThrowException() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(null)
                .endDate(null)
                .build();

        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date and end date are required");
    }
}

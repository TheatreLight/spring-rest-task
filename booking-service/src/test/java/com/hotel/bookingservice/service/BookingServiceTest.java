package com.hotel.bookingservice.service;

import com.hotel.bookingservice.client.HotelServiceClient;
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
    private HotelServiceClient hotelServiceClient;

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
        verify(hotelServiceClient, never()).confirmAvailability(anyLong(), any());
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
        when(hotelServiceClient.getRecommendedRooms(eq(1L), any(), any()))
                .thenReturn(Arrays.asList(recommendedRoom));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        when(hotelServiceClient.confirmAvailability(eq(2L), any())).thenReturn(availabilityResponse);
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(testBookingDto);

        // When
        BookingDto result = bookingService.createBooking(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(hotelServiceClient).getRecommendedRooms(eq(1L), any(), any());
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
        when(hotelServiceClient.getRecommendedRooms(eq(1L), any(), any()))
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
        verify(hotelServiceClient).releaseRoom(eq(1L), any(ReleaseRoomRequest.class));
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
        verify(hotelServiceClient, never()).releaseRoom(anyLong(), any());
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
}

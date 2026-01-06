package com.hotel.bookingservice.service;

import com.hotel.bookingservice.client.HotelServiceClient;
import com.hotel.bookingservice.dto.*;
import com.hotel.bookingservice.exception.HotelServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceCallerTest {

    @Mock
    private HotelServiceClient hotelServiceClient;

    private HotelServiceCaller hotelServiceCaller;

    @BeforeEach
    void setUp() {
        hotelServiceCaller = new HotelServiceCaller(hotelServiceClient);
    }

    @Test
    void getRecommendedRooms_ShouldDelegateToClient() {
        // Given
        Long hotelId = 1L;
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        RoomDto room = RoomDto.builder()
                .id(1L)
                .hotelId(1L)
                .number("101")
                .timesBooked(0)
                .build();

        when(hotelServiceClient.getRecommendedRooms(hotelId, startDate, endDate))
                .thenReturn(Arrays.asList(room));

        // When
        List<RoomDto> result = hotelServiceCaller.getRecommendedRooms(hotelId, startDate, endDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(hotelServiceClient).getRecommendedRooms(hotelId, startDate, endDate);
    }

    @Test
    void getRoomById_ShouldDelegateToClient() {
        // Given
        Long roomId = 1L;
        RoomDto room = RoomDto.builder()
                .id(1L)
                .hotelId(1L)
                .number("101")
                .build();

        when(hotelServiceClient.getRoomById(roomId)).thenReturn(room);

        // When
        RoomDto result = hotelServiceCaller.getRoomById(roomId);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        verify(hotelServiceClient).getRoomById(roomId);
    }

    @Test
    void confirmAvailability_ShouldDelegateToClient() {
        // Given
        Long roomId = 1L;
        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .requestId("test-request")
                .bookingId(1L)
                .build();

        AvailabilityResponse response = AvailabilityResponse.builder()
                .roomId(1L)
                .confirmed(true)
                .build();

        when(hotelServiceClient.confirmAvailability(roomId, request)).thenReturn(response);

        // When
        AvailabilityResponse result = hotelServiceCaller.confirmAvailability(roomId, request);

        // Then
        assertThat(result.isConfirmed()).isTrue();
        verify(hotelServiceClient).confirmAvailability(roomId, request);
    }

    @Test
    void confirmBooking_ShouldDelegateToClient() {
        // Given
        Long roomId = 1L;
        String requestId = "test-request";

        // When
        hotelServiceCaller.confirmBooking(roomId, requestId);

        // Then
        verify(hotelServiceClient).confirmBooking(roomId, requestId);
    }

    @Test
    void releaseRoom_ShouldDelegateToClient() {
        // Given
        Long roomId = 1L;
        ReleaseRoomRequest request = ReleaseRoomRequest.builder()
                .requestId("test-request")
                .bookingId(1L)
                .build();

        // When
        hotelServiceCaller.releaseRoom(roomId, request);

        // Then
        verify(hotelServiceClient).releaseRoom(roomId, request);
    }

    @Test
    void getRecommendedRooms_WhenClientThrows_ShouldPropagateException() {
        // Given
        when(hotelServiceClient.getRecommendedRooms(any(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        // When/Then
        assertThatThrownBy(() -> hotelServiceCaller.getRecommendedRooms(1L, LocalDate.now(), LocalDate.now().plusDays(1)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void confirmAvailability_WhenClientThrows_ShouldPropagateException() {
        // Given
        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .requestId("test")
                .build();

        when(hotelServiceClient.confirmAvailability(anyLong(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When/Then
        assertThatThrownBy(() -> hotelServiceCaller.confirmAvailability(1L, request))
                .isInstanceOf(RuntimeException.class);
    }
}

package com.hotel.hotelservice.service;

import com.hotel.hotelservice.dto.*;
import com.hotel.hotelservice.entity.*;
import com.hotel.hotelservice.exception.*;
import com.hotel.hotelservice.mapper.HotelMapper;
import com.hotel.hotelservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomLockRepository roomLockRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private RoomService roomService;

    private Room testRoom;
    private Hotel testHotel;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder()
                .id(1L)
                .name("Test Hotel")
                .address("123 Test St")
                .build();

        testRoom = Room.builder()
                .id(1L)
                .hotel(testHotel)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();
    }

    @Test
    void confirmAvailability_WithValidRequest_ShouldConfirmAndLock() {
        // Given
        String requestId = UUID.randomUUID().toString();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .requestId(requestId)
                .bookingId(1L)
                .build();

        when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRoom));
        when(roomLockRepository.existsOverlappingLock(eq(1L), any(), any())).thenReturn(false);
        when(roomLockRepository.save(any(RoomLock.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        AvailabilityResponse response = roomService.confirmAvailability(1L, request);

        // Then
        assertThat(response.isConfirmed()).isTrue();
        assertThat(response.getRoomId()).isEqualTo(1L);
        assertThat(response.getRequestId()).isEqualTo(requestId);
        verify(roomLockRepository).save(any(RoomLock.class));
    }

    @Test
    void confirmAvailability_WithIdempotentRequest_ShouldReturnExisting() {
        // Given
        String requestId = UUID.randomUUID().toString();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .requestId(requestId)
                .build();

        RoomLock existingLock = RoomLock.builder()
                .id(1L)
                .room(testRoom)
                .startDate(startDate)
                .endDate(endDate)
                .requestId(requestId)
                .build();

        when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.of(existingLock));

        // When
        AvailabilityResponse response = roomService.confirmAvailability(1L, request);

        // Then
        assertThat(response.isConfirmed()).isTrue();
        assertThat(response.getMessage()).contains("idempotent");
        verify(roomRepository, never()).findByIdWithLock(anyLong());
    }

    @Test
    void confirmAvailability_WithOverlappingDates_ShouldThrowException() {
        // Given
        String requestId = UUID.randomUUID().toString();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .requestId(requestId)
                .build();

        when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRoom));
        when(roomLockRepository.existsOverlappingLock(eq(1L), any(), any())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> roomService.confirmAvailability(1L, request))
                .isInstanceOf(RoomNotAvailableException.class);
    }

    @Test
    void confirmAvailability_WithUnavailableRoom_ShouldThrowException() {
        // Given
        testRoom.setAvailable(false);
        String requestId = UUID.randomUUID().toString();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .requestId(requestId)
                .build();

        when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRoom));

        // When/Then
        assertThatThrownBy(() -> roomService.confirmAvailability(1L, request))
                .isInstanceOf(RoomNotAvailableException.class)
                .hasMessageContaining("not operationally available");
    }

    @Test
    void confirmAvailability_WithInvalidDates_ShouldThrowException() {
        // Given
        String requestId = UUID.randomUUID().toString();
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(3); // End before start

        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .requestId(requestId)
                .build();

        when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> roomService.confirmAvailability(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date cannot be before start date");
    }

    @Test
    void releaseRoom_ShouldDeleteLock() {
        // Given
        String requestId = UUID.randomUUID().toString();
        ReleaseRoomRequest request = ReleaseRoomRequest.builder()
                .requestId(requestId)
                .build();

        RoomLock lock = RoomLock.builder()
                .id(1L)
                .room(testRoom)
                .requestId(requestId)
                .confirmed(false)
                .build();

        when(roomLockRepository.findByRoomIdAndRequestId(1L, requestId)).thenReturn(Optional.of(lock));

        // When
        roomService.releaseRoom(1L, request);

        // Then
        verify(roomLockRepository).delete(lock);
    }

    @Test
    void releaseRoom_WithNonExistentLock_ShouldNotThrow() {
        // Given
        String requestId = UUID.randomUUID().toString();
        ReleaseRoomRequest request = ReleaseRoomRequest.builder()
                .requestId(requestId)
                .build();

        when(roomLockRepository.findByRoomIdAndRequestId(1L, requestId)).thenReturn(Optional.empty());

        // When/Then - Should not throw
        assertThatCode(() -> roomService.releaseRoom(1L, request))
                .doesNotThrowAnyException();
    }

    @Test
    void confirmBooking_ShouldIncrementTimesBooked() {
        // Given
        String requestId = UUID.randomUUID().toString();
        RoomLock lock = RoomLock.builder()
                .id(1L)
                .room(testRoom)
                .requestId(requestId)
                .confirmed(false)
                .build();

        when(roomLockRepository.findByRoomIdAndRequestId(1L, requestId)).thenReturn(Optional.of(lock));
        when(roomLockRepository.save(any(RoomLock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        roomService.confirmBooking(1L, requestId);

        // Then
        assertThat(lock.getConfirmed()).isTrue();
        assertThat(testRoom.getTimesBooked()).isEqualTo(1);
        verify(roomLockRepository).save(lock);
        verify(roomRepository).save(testRoom);
    }
}

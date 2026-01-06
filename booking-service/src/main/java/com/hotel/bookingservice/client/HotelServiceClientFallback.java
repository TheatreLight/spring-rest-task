package com.hotel.bookingservice.client;

import com.hotel.bookingservice.dto.*;
import com.hotel.bookingservice.exception.HotelServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class HotelServiceClientFallback implements HotelServiceClient {

    @Override
    public List<RoomDto> getRecommendedRooms(Long hotelId, LocalDate startDate, LocalDate endDate) {
        log.error("Fallback: Hotel service is unavailable - getRecommendedRooms");
        throw new HotelServiceException("Hotel service is currently unavailable. Please try again later.");
    }

    @Override
    public RoomDto getRoomById(Long id) {
        log.error("Fallback: Hotel service is unavailable - getRoomById");
        throw new HotelServiceException("Hotel service is currently unavailable. Please try again later.");
    }

    @Override
    public AvailabilityResponse confirmAvailability(Long id, ConfirmAvailabilityRequest request) {
        log.error("Fallback: Hotel service is unavailable - confirmAvailability");
        throw new HotelServiceException("Hotel service is currently unavailable. Please try again later.");
    }

    @Override
    public void confirmBooking(Long id, String requestId) {
        log.error("Fallback: Hotel service is unavailable - confirmBooking");
        throw new HotelServiceException("Hotel service is currently unavailable. Please try again later.");
    }

    @Override
    public void releaseRoom(Long id, ReleaseRoomRequest request) {
        log.error("Fallback: Hotel service is unavailable - releaseRoom");
        // Compensation should still be attempted, log error but don't throw
        log.warn("Failed to release room {} with requestId {}. Manual intervention may be required.",
                id, request.getRequestId());
    }
}

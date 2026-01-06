package com.hotel.bookingservice.service;

import com.hotel.bookingservice.client.HotelServiceClient;
import com.hotel.bookingservice.dto.*;
import com.hotel.bookingservice.exception.HotelServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Service wrapper for Hotel Service calls with Resilience4j patterns.
 * Annotations must be on public methods in a Spring-managed bean for AOP to work.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HotelServiceCaller {

    private final HotelServiceClient hotelServiceClient;

    @CircuitBreaker(name = "hotelService", fallbackMethod = "getRecommendedRoomsFallback")
    @Retry(name = "hotelService")
    public List<RoomDto> getRecommendedRooms(Long hotelId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching recommended rooms for hotel {} and dates {} - {}", hotelId, startDate, endDate);
        return hotelServiceClient.getRecommendedRooms(hotelId, startDate, endDate);
    }

    @CircuitBreaker(name = "hotelService", fallbackMethod = "getRoomByIdFallback")
    @Retry(name = "hotelService")
    public RoomDto getRoomById(Long roomId) {
        log.debug("Fetching room by id: {}", roomId);
        return hotelServiceClient.getRoomById(roomId);
    }

    @CircuitBreaker(name = "hotelService", fallbackMethod = "confirmAvailabilityFallback")
    @Retry(name = "hotelService")
    public AvailabilityResponse confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        log.debug("Confirming availability for room {} with requestId {}", roomId, request.getRequestId());
        return hotelServiceClient.confirmAvailability(roomId, request);
    }

    @Retry(name = "hotelService")
    public void confirmBooking(Long roomId, String requestId) {
        log.debug("Confirming booking for room {} with requestId {}", roomId, requestId);
        hotelServiceClient.confirmBooking(roomId, requestId);
    }

    @Retry(name = "hotelService")
    public void releaseRoom(Long roomId, ReleaseRoomRequest request) {
        log.debug("Releasing room {} with requestId {}", roomId, request.getRequestId());
        hotelServiceClient.releaseRoom(roomId, request);
    }

    // Fallback methods

    private List<RoomDto> getRecommendedRoomsFallback(Long hotelId, LocalDate startDate, LocalDate endDate, Exception e) {
        log.error("Circuit breaker fallback for getRecommendedRooms: {}", e.getMessage());
        throw new HotelServiceException("Hotel service is unavailable. Please try again later.", e);
    }

    private RoomDto getRoomByIdFallback(Long roomId, Exception e) {
        log.error("Circuit breaker fallback for getRoomById: {}", e.getMessage());
        throw new HotelServiceException("Hotel service is unavailable. Please try again later.", e);
    }

    private AvailabilityResponse confirmAvailabilityFallback(Long roomId, ConfirmAvailabilityRequest request, Exception e) {
        log.error("Circuit breaker fallback for confirmAvailability: {}", e.getMessage());
        throw new HotelServiceException("Hotel service is unavailable. Please try again later.", e);
    }
}

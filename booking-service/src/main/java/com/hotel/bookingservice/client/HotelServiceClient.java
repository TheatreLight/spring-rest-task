package com.hotel.bookingservice.client;

import com.hotel.bookingservice.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "hotel-service", fallback = HotelServiceClientFallback.class)
public interface HotelServiceClient {

    @GetMapping("/api/rooms/recommend")
    List<RoomDto> getRecommendedRooms(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate);

    @GetMapping("/api/rooms/{id}")
    RoomDto getRoomById(@PathVariable Long id);

    @PostMapping("/api/rooms/{id}/confirm-availability")
    AvailabilityResponse confirmAvailability(
            @PathVariable Long id,
            @RequestBody ConfirmAvailabilityRequest request);

    @PostMapping("/api/rooms/{id}/confirm-booking")
    void confirmBooking(
            @PathVariable Long id,
            @RequestParam String requestId);

    @PostMapping("/api/rooms/{id}/release")
    void releaseRoom(
            @PathVariable Long id,
            @RequestBody ReleaseRoomRequest request);
}

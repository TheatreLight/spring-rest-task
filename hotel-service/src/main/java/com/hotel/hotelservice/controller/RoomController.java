package com.hotel.hotelservice.controller;

import com.hotel.hotelservice.dto.*;
import com.hotel.hotelservice.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    @Operation(summary = "Get all available rooms")
    public ResponseEntity<List<RoomDto>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/hotel/{hotelId}")
    @Operation(summary = "Get rooms by hotel ID")
    public ResponseEntity<List<RoomDto>> getRoomsByHotel(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomService.getRoomsByHotel(hotelId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @GetMapping("/recommend")
    @Operation(summary = "Get recommended rooms sorted by times_booked (ASC)")
    public ResponseEntity<List<RoomDto>> getRecommendedRooms(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            if (hotelId != null) {
                return ResponseEntity.ok(roomService.getRecommendedRoomsForDatesByHotel(hotelId, startDate, endDate));
            }
            return ResponseEntity.ok(roomService.getRecommendedRoomsForDates(startDate, endDate));
        }
        return ResponseEntity.ok(roomService.getRecommendedRooms());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new room", description = "Admin only")
    public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomDto room = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a room", description = "Admin only")
    public ResponseEntity<RoomDto> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a room", description = "Admin only")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm-availability")
    @Operation(summary = "Confirm room availability (internal)", description = "Called by Booking Service")
    public ResponseEntity<AvailabilityResponse> confirmAvailability(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmAvailabilityRequest request) {
        AvailabilityResponse response = roomService.confirmAvailability(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-booking")
    @Operation(summary = "Confirm booking and increment times_booked (internal)")
    public ResponseEntity<Void> confirmBooking(
            @PathVariable Long id,
            @RequestParam String requestId) {
        roomService.confirmBooking(id, requestId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release room lock (compensation)", description = "Called by Booking Service for compensation")
    public ResponseEntity<Void> releaseRoom(
            @PathVariable Long id,
            @Valid @RequestBody ReleaseRoomRequest request) {
        roomService.releaseRoom(id, request);
        return ResponseEntity.ok().build();
    }
}

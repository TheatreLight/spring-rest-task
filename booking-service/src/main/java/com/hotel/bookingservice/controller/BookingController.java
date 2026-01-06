package com.hotel.bookingservice.controller;

import com.hotel.bookingservice.dto.*;
import com.hotel.bookingservice.security.UserPrincipal;
import com.hotel.bookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/booking")
    @Operation(summary = "Create a new booking", description = "Creates a booking with manual room selection or auto-select")
    public ResponseEntity<BookingDto> createBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingDto booking = bookingService.createBooking(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get user's booking history")
    public ResponseEntity<List<BookingDto>> getUserBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bookingService.getUserBookings(principal.getId()));
    }

    @GetMapping("/bookings/page")
    @Operation(summary = "Get user's booking history with pagination")
    public ResponseEntity<Page<BookingDto>> getUserBookingsPage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookingService.getUserBookings(principal.getId(), pageable));
    }

    @GetMapping("/booking/{id}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<BookingDto> getBookingById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id, principal.getId()));
    }

    @DeleteMapping("/booking/{id}")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<Void> cancelBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        bookingService.cancelBooking(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}

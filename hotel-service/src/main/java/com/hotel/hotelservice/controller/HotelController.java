package com.hotel.hotelservice.controller;

import com.hotel.hotelservice.dto.*;
import com.hotel.hotelservice.service.HotelService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotels", description = "Hotel management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class HotelController {

    private final HotelService hotelService;

    @GetMapping
    @Operation(summary = "Get all hotels", description = "Returns a list of all hotels")
    public ResponseEntity<List<HotelDto>> getAllHotels() {
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    @GetMapping("/page")
    @Operation(summary = "Get hotels with pagination")
    public ResponseEntity<Page<HotelDto>> getHotelsPage(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(hotelService.getHotels(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search hotels by name or address")
    public ResponseEntity<Page<HotelDto>> searchHotels(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(hotelService.searchHotels(query, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hotel by ID", description = "Returns a hotel with all its rooms")
    public ResponseEntity<HotelDto> getHotelById(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getHotelById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new hotel", description = "Admin only")
    public ResponseEntity<HotelDto> createHotel(@Valid @RequestBody CreateHotelRequest request) {
        HotelDto hotel = hotelService.createHotel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(hotel);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a hotel", description = "Admin only")
    public ResponseEntity<HotelDto> updateHotel(
            @PathVariable Long id,
            @Valid @RequestBody CreateHotelRequest request) {
        return ResponseEntity.ok(hotelService.updateHotel(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a hotel", description = "Admin only")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}

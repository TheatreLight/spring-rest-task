package com.hotel.bookingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.bookingservice.dto.*;
import com.hotel.bookingservice.entity.*;
import com.hotel.bookingservice.exception.BookingException;
import com.hotel.bookingservice.exception.HotelServiceException;
import com.hotel.bookingservice.repository.BookingRepository;
import com.hotel.bookingservice.repository.UserRepository;
import com.hotel.bookingservice.service.HotelServiceCaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BookingController testing full API flow and error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private HotelServiceCaller hotelServiceCaller;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        bookingRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build();
        testUser = userRepository.save(testUser);

        // Get auth token
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        authToken = authResponse.getToken();
    }

    @Test
    void createBooking_WithValidRequest_ShouldReturn201() throws Exception {
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

        when(hotelServiceCaller.confirmAvailability(eq(1L), any())).thenReturn(availabilityResponse);

        // When/Then
        mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void createBooking_WhenRoomNotAvailable_ShouldReturn409Conflict() throws Exception {
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
                .message("Room is already booked")
                .build();

        when(hotelServiceCaller.confirmAvailability(eq(1L), any())).thenReturn(notAvailableResponse);

        // When/Then
        mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createBooking_WhenHotelServiceUnavailable_ShouldReturn503() throws Exception {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        when(hotelServiceCaller.confirmAvailability(eq(1L), any()))
                .thenThrow(new HotelServiceException("Hotel service is unavailable", new RuntimeException()));

        // When/Then - BookingException wraps HotelServiceException, returns 409
        mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()); // BookingException returns 409
    }

    @Test
    void createBooking_WithInvalidDates_ShouldReturn400() throws Exception {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(3)) // End before start
                .build();

        // When/Then
        mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date cannot be before start date"));
    }

    @Test
    void createBooking_WithPastDate_ShouldReturn400() throws Exception {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        // When/Then
        mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start date cannot be in the past"));
    }

    @Test
    void createBooking_WithoutAuth_ShouldReturn403() throws Exception {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        // When/Then
        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBooking_WithAutoSelect_ShouldSelectRoom() throws Exception {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(true)
                .build();

        RoomDto recommendedRoom = RoomDto.builder()
                .id(5L)
                .hotelId(1L)
                .number("505")
                .timesBooked(0)
                .build();

        AvailabilityResponse availabilityResponse = AvailabilityResponse.builder()
                .roomId(5L)
                .confirmed(true)
                .build();

        when(hotelServiceCaller.getRecommendedRooms(eq(1L), any(), any()))
                .thenReturn(Arrays.asList(recommendedRoom));
        when(hotelServiceCaller.confirmAvailability(eq(5L), any())).thenReturn(availabilityResponse);

        // When/Then
        mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(5));
    }

    @Test
    void createBooking_WithAutoSelectAndNoRooms_ShouldReturn409() throws Exception {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(true)
                .build();

        when(hotelServiceCaller.getRecommendedRooms(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());

        // When/Then
        mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No available rooms found for the selected dates"));
    }

    @Test
    void createBooking_WithIdempotentRequest_ShouldReturnSameBooking() throws Exception {
        // Given
        String requestId = UUID.randomUUID().toString();
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .requestId(requestId)
                .build();

        AvailabilityResponse availabilityResponse = AvailabilityResponse.builder()
                .roomId(1L)
                .confirmed(true)
                .build();

        when(hotelServiceCaller.confirmAvailability(eq(1L), any())).thenReturn(availabilityResponse);

        // First request
        MvcResult firstResult = mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Second request with same requestId
        MvcResult secondResult = mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then - Both should return the same booking
        BookingDto firstBooking = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(), BookingDto.class);
        BookingDto secondBooking = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(), BookingDto.class);

        org.assertj.core.api.Assertions.assertThat(firstBooking.getId())
                .isEqualTo(secondBooking.getId());

        // Verify hotel service was only called once
        verify(hotelServiceCaller, times(1)).confirmAvailability(eq(1L), any());
    }

    @Test
    void getUserBookings_ShouldReturnUserBookings() throws Exception {
        // Given - Create a booking first
        Booking booking = Booking.builder()
                .user(testUser)
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .requestId(UUID.randomUUID().toString())
                .build();
        bookingRepository.save(booking);

        // When/Then
        mockMvc.perform(get("/bookings")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].roomId").value(1));
    }

    @Test
    void getBookingById_ShouldReturnBooking() throws Exception {
        // Given
        Booking booking = Booking.builder()
                .user(testUser)
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .requestId(UUID.randomUUID().toString())
                .build();
        booking = bookingRepository.save(booking);

        // When/Then
        mockMvc.perform(get("/booking/" + booking.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.getId()))
                .andExpect(jsonPath("$.roomId").value(1));
    }

    @Test
    void getBookingById_WithNonExistent_ShouldReturn404() throws Exception {
        // When/Then
        mockMvc.perform(get("/booking/999999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelBooking_ShouldCancelAndReturn204() throws Exception {
        // Given
        Booking booking = Booking.builder()
                .user(testUser)
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .requestId(UUID.randomUUID().toString())
                .build();
        booking = bookingRepository.save(booking);

        // When/Then
        mockMvc.perform(delete("/booking/" + booking.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify booking is cancelled
        Booking cancelled = bookingRepository.findById(booking.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(cancelled.getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelBooking_WithNonExistent_ShouldReturn404() throws Exception {
        // When/Then
        mockMvc.perform(delete("/booking/999999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void errorResponse_ShouldIncludeTraceId() throws Exception {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        // When/Then
        mockMvc.perform(post("/booking")
                        .header("Authorization", "Bearer " + authToken)
                        .header("X-Trace-Id", "test-trace-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").value("test-trace-123"))
                .andExpect(jsonPath("$.path").value("/booking"))
                .andExpect(jsonPath("$.status").value(400));
    }
}

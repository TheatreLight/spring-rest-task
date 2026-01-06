package com.hotel.hotelservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.hotelservice.dto.CreateHotelRequest;
import com.hotel.hotelservice.dto.HotelDto;
import com.hotel.hotelservice.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HotelService hotelService;

    private HotelDto testHotel;

    @BeforeEach
    void setUp() {
        testHotel = HotelDto.builder()
                .id(1L)
                .name("Test Hotel")
                .address("123 Test Street")
                .totalRooms(10)
                .availableRooms(8)
                .build();
    }

    @Test
    void getAllHotels_ShouldReturnHotelList() throws Exception {
        List<HotelDto> hotels = Arrays.asList(testHotel);
        when(hotelService.getAllHotels()).thenReturn(hotels);

        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Test Hotel"));
    }

    @Test
    void getHotelById_ShouldReturnHotel() throws Exception {
        when(hotelService.getHotelById(1L)).thenReturn(testHotel);

        mockMvc.perform(get("/api/hotels/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Test Hotel"))
                .andExpect(jsonPath("$.address").value("123 Test Street"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createHotel_WithAdminRole_ShouldCreateHotel() throws Exception {
        CreateHotelRequest request = CreateHotelRequest.builder()
                .name("New Hotel")
                .address("456 New Street")
                .build();

        HotelDto createdHotel = HotelDto.builder()
                .id(2L)
                .name("New Hotel")
                .address("456 New Street")
                .build();

        when(hotelService.createHotel(any(CreateHotelRequest.class))).thenReturn(createdHotel);

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Hotel"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createHotel_WithUserRole_ShouldReturnForbidden() throws Exception {
        CreateHotelRequest request = CreateHotelRequest.builder()
                .name("New Hotel")
                .address("456 New Street")
                .build();

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createHotel_WithoutAuth_ShouldReturnForbidden() throws Exception {
        CreateHotelRequest request = CreateHotelRequest.builder()
                .name("New Hotel")
                .address("456 New Street")
                .build();

        // Spring Security returns 403 for unauthenticated requests to protected endpoints
        // when no custom AuthenticationEntryPoint is configured
        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createHotel_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        CreateHotelRequest request = CreateHotelRequest.builder()
                .name("") // Invalid: empty name
                .build();

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

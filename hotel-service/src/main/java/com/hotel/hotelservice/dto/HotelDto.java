package com.hotel.hotelservice.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelDto {
    private Long id;
    private String name;
    private String address;
    private List<RoomDto> rooms;
    private Integer totalRooms;
    private Integer availableRooms;
}

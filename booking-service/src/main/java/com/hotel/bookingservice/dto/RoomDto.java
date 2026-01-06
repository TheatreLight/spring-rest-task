package com.hotel.bookingservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDto {
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String number;
    private Boolean available;
    private Integer timesBooked;
}

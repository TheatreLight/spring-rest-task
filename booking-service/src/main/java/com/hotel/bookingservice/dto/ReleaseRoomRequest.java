package com.hotel.bookingservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseRoomRequest {
    private String requestId;
    private Long bookingId;
}

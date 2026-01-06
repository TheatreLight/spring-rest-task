package com.hotel.hotelservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseRoomRequest {

    @NotBlank(message = "Request ID is required")
    private String requestId;

    private Long bookingId;
}

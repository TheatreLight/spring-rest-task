package com.hotel.hotelservice.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {
    private Long roomId;
    private String requestId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean confirmed;
    private String message;
}

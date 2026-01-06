package com.hotel.bookingservice.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmAvailabilityRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String requestId;
    private Long bookingId;
}

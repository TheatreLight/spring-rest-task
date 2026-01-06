package com.hotel.bookingservice.dto;

import com.hotel.bookingservice.entity.BookingStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDto {
    private Long id;
    private Long userId;
    private String username;
    private Long roomId;
    private Long hotelId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private String requestId;
    private LocalDateTime createdAt;
}

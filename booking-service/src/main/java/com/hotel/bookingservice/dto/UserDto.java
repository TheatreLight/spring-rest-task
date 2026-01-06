package com.hotel.bookingservice.dto;

import com.hotel.bookingservice.entity.Role;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String username;
    private Role role;
    private LocalDateTime createdAt;
}

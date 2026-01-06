package com.hotel.bookingservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String type;
    private Long userId;
    private String username;
    private String role;
    private Long expiresIn;
}

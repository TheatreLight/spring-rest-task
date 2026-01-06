package com.hotel.hotelservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHotelRequest {

    @NotBlank(message = "Hotel name is required")
    @Size(min = 2, max = 255, message = "Hotel name must be between 2 and 255 characters")
    private String name;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
}

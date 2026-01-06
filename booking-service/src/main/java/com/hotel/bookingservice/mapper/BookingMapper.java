package com.hotel.bookingservice.mapper;

import com.hotel.bookingservice.dto.*;
import com.hotel.bookingservice.entity.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    BookingDto toDto(Booking booking);

    List<BookingDto> toDtoList(List<Booking> bookings);
}

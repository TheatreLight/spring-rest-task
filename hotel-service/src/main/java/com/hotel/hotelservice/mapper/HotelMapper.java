package com.hotel.hotelservice.mapper;

import com.hotel.hotelservice.dto.*;
import com.hotel.hotelservice.entity.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HotelMapper {

    @Named("toFullDto")
    @Mapping(target = "totalRooms", expression = "java(hotel.getRooms() != null ? hotel.getRooms().size() : 0)")
    @Mapping(target = "availableRooms", expression = "java(hotel.getRooms() != null ? (int) hotel.getRooms().stream().filter(Room::getAvailable).count() : 0)")
    HotelDto toDto(Hotel hotel);

    @Named("toDtoWithoutRooms")
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "totalRooms", constant = "0")
    @Mapping(target = "availableRooms", constant = "0")
    HotelDto toDtoWithoutRooms(Hotel hotel);

    @IterableMapping(qualifiedByName = "toDtoWithoutRooms")
    List<HotelDto> toDtoList(List<Hotel> hotels);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    Hotel toEntity(CreateHotelRequest request);

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    RoomDto toRoomDto(Room room);

    List<RoomDto> toRoomDtoList(List<Room> rooms);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "timesBooked", constant = "0")
    @Mapping(target = "version", ignore = true)
    Room toEntity(CreateRoomRequest request);
}

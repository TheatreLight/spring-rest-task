package com.hotel.hotelservice.exception;

public class RoomNotAvailableException extends RuntimeException {

    public RoomNotAvailableException(String message) {
        super(message);
    }

    public RoomNotAvailableException(Long roomId) {
        super(String.format("Room with id %d is not available for the requested dates", roomId));
    }
}

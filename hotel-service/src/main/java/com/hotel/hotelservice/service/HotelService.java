package com.hotel.hotelservice.service;

import com.hotel.hotelservice.dto.*;
import com.hotel.hotelservice.entity.Hotel;
import com.hotel.hotelservice.exception.DuplicateResourceException;
import com.hotel.hotelservice.exception.ResourceNotFoundException;
import com.hotel.hotelservice.mapper.HotelMapper;
import com.hotel.hotelservice.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    public List<HotelDto> getAllHotels() {
        log.debug("Fetching all hotels");
        List<Hotel> hotels = hotelRepository.findAll();
        return hotelMapper.toDtoList(hotels);
    }

    public Page<HotelDto> getHotels(Pageable pageable) {
        log.debug("Fetching hotels with pagination: {}", pageable);
        return hotelRepository.findAll(pageable)
                .map(hotelMapper::toDtoWithoutRooms);
    }

    public Page<HotelDto> searchHotels(String search, Pageable pageable) {
        log.debug("Searching hotels with term: {}", search);
        return hotelRepository.searchByNameOrAddress(search, pageable)
                .map(hotelMapper::toDtoWithoutRooms);
    }

    public HotelDto getHotelById(Long id) {
        log.debug("Fetching hotel by id: {}", id);
        Hotel hotel = hotelRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", id));
        return hotelMapper.toDto(hotel);
    }

    @Transactional
    public HotelDto createHotel(CreateHotelRequest request) {
        log.info("Creating new hotel: {}", request.getName());

        hotelRepository.findByName(request.getName())
                .ifPresent(h -> {
                    throw new DuplicateResourceException("Hotel with name '" + request.getName() + "' already exists");
                });

        Hotel hotel = hotelMapper.toEntity(request);
        hotel = hotelRepository.save(hotel);

        log.info("Hotel created successfully with id: {}", hotel.getId());
        return hotelMapper.toDto(hotel);
    }

    @Transactional
    public HotelDto updateHotel(Long id, CreateHotelRequest request) {
        log.info("Updating hotel with id: {}", id);

        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", id));

        hotelRepository.findByName(request.getName())
                .filter(h -> !h.getId().equals(id))
                .ifPresent(h -> {
                    throw new DuplicateResourceException("Hotel with name '" + request.getName() + "' already exists");
                });

        hotel.setName(request.getName());
        hotel.setAddress(request.getAddress());
        hotel = hotelRepository.save(hotel);

        log.info("Hotel updated successfully: {}", id);
        return hotelMapper.toDto(hotel);
    }

    @Transactional
    public void deleteHotel(Long id) {
        log.info("Deleting hotel with id: {}", id);

        if (!hotelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hotel", id);
        }

        hotelRepository.deleteById(id);
        log.info("Hotel deleted successfully: {}", id);
    }
}

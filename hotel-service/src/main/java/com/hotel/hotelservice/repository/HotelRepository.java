package com.hotel.hotelservice.repository;

import com.hotel.hotelservice.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    Optional<Hotel> findByName(String name);

    @Query("SELECT h FROM Hotel h WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(h.address) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Hotel> searchByNameOrAddress(@Param("search") String search, Pageable pageable);

    @Query("SELECT h FROM Hotel h LEFT JOIN FETCH h.rooms WHERE h.id = :id")
    Optional<Hotel> findByIdWithRooms(@Param("id") Long id);

    List<Hotel> findByAddressContainingIgnoreCase(String address);
}

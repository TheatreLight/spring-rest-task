package com.hotel.hotelservice.repository;

import com.hotel.hotelservice.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    Page<Room> findByHotelId(Long hotelId, Pageable pageable);

    List<Room> findByAvailableTrue();

    @Query("SELECT r FROM Room r WHERE r.available = true ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAvailableRoomsSortedByTimesBooked();

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.available = true ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAvailableRoomsByHotelSortedByTimesBooked(@Param("hotelId") Long hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT r FROM Room r WHERE r.available = true AND r.id NOT IN " +
           "(SELECT rl.room.id FROM RoomLock rl WHERE " +
           "((rl.startDate <= :endDate AND rl.endDate >= :startDate) OR " +
           "(rl.startDate >= :startDate AND rl.startDate <= :endDate))) " +
           "ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAvailableRoomsForDates(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.available = true AND r.id NOT IN " +
           "(SELECT rl.room.id FROM RoomLock rl WHERE " +
           "((rl.startDate <= :endDate AND rl.endDate >= :startDate) OR " +
           "(rl.startDate >= :startDate AND rl.startDate <= :endDate))) " +
           "ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAvailableRoomsForDatesByHotel(@Param("hotelId") Long hotelId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    Optional<Room> findByHotelIdAndNumber(Long hotelId, String number);
}

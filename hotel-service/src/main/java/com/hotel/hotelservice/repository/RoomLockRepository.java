package com.hotel.hotelservice.repository;

import com.hotel.hotelservice.entity.RoomLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomLockRepository extends JpaRepository<RoomLock, Long> {

    Optional<RoomLock> findByRequestId(String requestId);

    List<RoomLock> findByRoomId(Long roomId);

    Optional<RoomLock> findByRoomIdAndRequestId(Long roomId, String requestId);

    @Query("SELECT rl FROM RoomLock rl WHERE rl.room.id = :roomId AND " +
           "((rl.startDate <= :endDate AND rl.endDate >= :startDate))")
    List<RoomLock> findOverlappingLocks(@Param("roomId") Long roomId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT CASE WHEN COUNT(rl) > 0 THEN true ELSE false END FROM RoomLock rl " +
           "WHERE rl.room.id = :roomId AND " +
           "((rl.startDate <= :endDate AND rl.endDate >= :startDate))")
    boolean existsOverlappingLock(@Param("roomId") Long roomId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    void deleteByRequestId(String requestId);

    void deleteByBookingId(Long bookingId);

    @Query("DELETE FROM RoomLock rl WHERE rl.confirmed = false AND rl.createdAt < :threshold")
    void deleteExpiredUnconfirmedLocks(@Param("threshold") LocalDateTime threshold);
}

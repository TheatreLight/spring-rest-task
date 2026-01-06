package com.hotel.bookingservice.repository;

import com.hotel.bookingservice.entity.Booking;
import com.hotel.bookingservice.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByRequestId(String requestId);

    List<Booking> findByUserId(Long userId);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId AND b.status != 'CANCELLED' AND " +
           "((b.startDate <= :endDate AND b.endDate >= :startDate))")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.roomId = :roomId AND b.status != 'CANCELLED' AND " +
           "((b.startDate <= :endDate AND b.endDate >= :startDate))")
    boolean existsOverlappingBooking(@Param("roomId") Long roomId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    Page<Booking> findUserBookingsOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    List<Booking> findByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.createdAt < :threshold")
    List<Booking> findStalePendingBookings(@Param("threshold") java.time.LocalDateTime threshold);
}

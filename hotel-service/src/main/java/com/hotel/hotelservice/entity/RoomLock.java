package com.hotel.hotelservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_locks", indexes = {
    @Index(name = "idx_room_lock_room_dates", columnList = "room_id, start_date, end_date"),
    @Index(name = "idx_room_lock_request", columnList = "request_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "confirmed")
    @Builder.Default
    private Boolean confirmed = false;
}

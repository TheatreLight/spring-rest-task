package com.hotel.hotelservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private String number;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "times_booked")
    @Builder.Default
    private Integer timesBooked = 0;

    @Version
    private Long version;

    public void incrementTimesBooked() {
        this.timesBooked++;
    }
}

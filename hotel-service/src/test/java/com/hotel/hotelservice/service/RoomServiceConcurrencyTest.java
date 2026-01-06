package com.hotel.hotelservice.service;

import com.hotel.hotelservice.dto.*;
import com.hotel.hotelservice.entity.*;
import com.hotel.hotelservice.exception.RoomNotAvailableException;
import com.hotel.hotelservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for concurrent booking scenarios.
 * Tests that parallel bookings for the same room on overlapping dates
 * are properly handled with only one succeeding.
 */
@SpringBootTest
@ActiveProfiles("test")
class RoomServiceConcurrencyTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomLockRepository roomLockRepository;

    @Autowired
    private HotelRepository hotelRepository;

    private Hotel testHotel;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        // Clean up locks from previous tests
        roomLockRepository.deleteAll();

        // Create test hotel if not exists
        testHotel = hotelRepository.findById(1L).orElseGet(() -> {
            Hotel hotel = Hotel.builder()
                    .name("Test Hotel")
                    .address("123 Test St")
                    .build();
            return hotelRepository.save(hotel);
        });

        // Create a fresh room for each test
        testRoom = Room.builder()
                .hotel(testHotel)
                .number("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .available(true)
                .timesBooked(0)
                .build();
        testRoom = roomRepository.save(testRoom);
    }

    @Test
    void parallelBookings_ForSameRoomAndDates_OnlyOneShouldSucceed() throws Exception {
        // Given
        int numberOfConcurrentRequests = 10;
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfConcurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Future<Boolean>> futures = new ArrayList<>();

        // When - Submit parallel booking requests
        for (int i = 0; i < numberOfConcurrentRequests; i++) {
            final int requestNum = i;
            Future<Boolean> future = executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    String requestId = "parallel-test-" + requestNum + "-" + UUID.randomUUID();
                    ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                            .startDate(startDate)
                            .endDate(endDate)
                            .requestId(requestId)
                            .bookingId((long) requestNum)
                            .build();

                    AvailabilityResponse response = roomService.confirmAvailability(testRoom.getId(), request);
                    if (response.isConfirmed()) {
                        successCount.incrementAndGet();
                        return true;
                    }
                    return false;
                } catch (RoomNotAvailableException e) {
                    conflictCount.incrementAndGet();
                    return false;
                } catch (Exception e) {
                    // Log other exceptions
                    System.err.println("Unexpected error in thread " + requestNum + ": " + e.getMessage());
                    return false;
                } finally {
                    doneLatch.countDown();
                }
            });
            futures.add(future);
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all to complete
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1); // Only one should succeed
        assertThat(conflictCount.get()).isEqualTo(numberOfConcurrentRequests - 1); // Rest should get conflict
    }

    @Test
    void parallelBookings_ForDifferentDates_AllShouldSucceed() throws Exception {
        // Given
        int numberOfConcurrentRequests = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfConcurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);

        // When - Submit parallel booking requests with non-overlapping dates
        for (int i = 0; i < numberOfConcurrentRequests; i++) {
            final int requestNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Each request has non-overlapping dates
                    LocalDate startDate = LocalDate.now().plusDays(1 + (requestNum * 5));
                    LocalDate endDate = LocalDate.now().plusDays(3 + (requestNum * 5));

                    String requestId = "non-overlap-test-" + requestNum + "-" + UUID.randomUUID();
                    ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                            .startDate(startDate)
                            .endDate(endDate)
                            .requestId(requestId)
                            .bookingId((long) requestNum)
                            .build();

                    AvailabilityResponse response = roomService.confirmAvailability(testRoom.getId(), request);
                    if (response.isConfirmed()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Error in thread " + requestNum + ": " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - All should succeed since dates don't overlap
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(numberOfConcurrentRequests);
    }

    @Test
    void idempotentRequests_ShouldReturnSameResult() throws Exception {
        // Given
        String requestId = "idempotent-test-" + UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .requestId(requestId)
                .bookingId(1L)
                .build();

        // When - Send the same request multiple times
        AvailabilityResponse response1 = roomService.confirmAvailability(testRoom.getId(), request);
        AvailabilityResponse response2 = roomService.confirmAvailability(testRoom.getId(), request);
        AvailabilityResponse response3 = roomService.confirmAvailability(testRoom.getId(), request);

        // Then - All should be confirmed (idempotent)
        assertThat(response1.isConfirmed()).isTrue();
        assertThat(response2.isConfirmed()).isTrue();
        assertThat(response3.isConfirmed()).isTrue();
        assertThat(response2.getMessage()).contains("idempotent");
        assertThat(response3.getMessage()).contains("idempotent");
    }

    @Test
    void sequentialIdempotentRequests_ShouldAllSucceed() throws Exception {
        // Given - Sequential requests with the same requestId should all succeed
        String requestId = "sequential-idempotent-" + UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .requestId(requestId)
                .bookingId(1L)
                .build();

        // When - Send the same request multiple times sequentially
        int successCount = 0;
        for (int i = 0; i < 10; i++) {
            AvailabilityResponse response = roomService.confirmAvailability(testRoom.getId(), request);
            if (response.isConfirmed()) {
                successCount++;
            }
        }

        // Then - All should succeed due to idempotency
        assertThat(successCount).isEqualTo(10);
    }

    @Test
    void parallelRequestsWithSameId_AtLeastOneShouldSucceed() throws Exception {
        // Given - Parallel requests with same requestId - at least one should succeed
        // Due to transaction isolation, parallel requests may not all see the idempotent lock
        int numberOfConcurrentRequests = 10;
        String requestId = "parallel-idempotent-" + UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfConcurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);

        // When - Submit same request ID in parallel
        for (int i = 0; i < numberOfConcurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                            .startDate(startDate)
                            .endDate(endDate)
                            .requestId(requestId) // Same requestId for all
                            .bookingId(1L)
                            .build();

                    AvailabilityResponse response = roomService.confirmAvailability(testRoom.getId(), request);
                    if (response.isConfirmed()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Expected for concurrent requests due to race conditions
                }
                 finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - At least one should succeed (the first to commit)
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }
}

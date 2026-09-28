package com.smartbooking.domain.service;

import com.smartbooking.domain.model.Booking;
import com.smartbooking.domain.model.Resource;
import com.smartbooking.domain.model.Role;
import com.smartbooking.domain.model.User;
import com.smartbooking.domain.repository.BookingRepository;
import com.smartbooking.domain.repository.ResourceRepository;
import com.smartbooking.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void shouldAllowOnlyOneBookingWhenTwoUsersBookSameSlot() throws Exception {

        Resource resource = Resource.builder()
                .name("Salle test concurrence")
                .type("ROOM")
                .description("Ressource utilisée pour le test de concurrence")
                .capacity(1)
                .isActive(true)
                .build();

        Resource savedResource = resourceRepository.save(resource);

        User user1 = User.builder()
                .email("concurrency1-" + System.nanoTime() + "@test.com")
                .password("password")
                .name("Concurrency User 1")
                .role(Role.USER)
                .build();

        User user2 = User.builder()
                .email("concurrency2-" + System.nanoTime() + "@test.com")
                .password("password")
                .name("Concurrency User 2")
                .role(Role.USER)
                .build();

        User savedUser1 = userRepository.save(user1);
        User savedUser2 = userRepository.save(user2);

        LocalDateTime start =
                LocalDateTime.of(2030, 1, 10, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2030, 1, 10, 11, 0);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startSignal =
                new CountDownLatch(1);

        Callable<Booking> booking1 = () -> {
            startSignal.await();

            return bookingService.createBooking(
                    savedUser1.getId(),
                    savedResource.getId(),
                    start,
                    end,
                    "concurrency-key-1-" + System.nanoTime(),
                    "Test concurrence utilisateur 1"
            );
        };

        Callable<Booking> booking2 = () -> {
            startSignal.await();

            return bookingService.createBooking(
                    savedUser2.getId(),
                    savedResource.getId(),
                    start,
                    end,
                    "concurrency-key-2-" + System.nanoTime(),
                    "Test concurrence utilisateur 2"
            );
        };

        Future<Booking> future1 = executor.submit(booking1);
        Future<Booking> future2 = executor.submit(booking2);

        startSignal.countDown();

        int successfulBookings = 0;

        try {
            future1.get();
            successfulBookings++;
        } catch (ExecutionException e) {
            // Une réservation doit échouer à cause du conflit
        }

        try {
            future2.get();
            successfulBookings++;
        } catch (ExecutionException e) {
            // Une réservation doit échouer à cause du conflit
        }

        executor.shutdown();

        assertEquals(1, successfulBookings);

        long bookingsForSlot = bookingRepository.findByResourceId(savedResource.getId())
                .stream()
                .filter(b -> b.getStartTime().isBefore(end)
                        && b.getEndTime().isAfter(start))
                .count();

        assertEquals(1, bookingsForSlot);
    }
}
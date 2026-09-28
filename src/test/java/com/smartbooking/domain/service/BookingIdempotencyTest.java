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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BookingIdempotencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void shouldReturnSameBookingWhenSameIdempotencyKeyIsUsedTwice() {

        Resource resource = Resource.builder()
                .name("Salle test idempotence")
                .type("ROOM")
                .description("Ressource utilisée pour le test d'idempotence")
                .capacity(1)
                .isActive(true)
                .build();

        Resource savedResource = resourceRepository.save(resource);

        User user = User.builder()
                .email("idempotency-" + System.nanoTime() + "@test.com")
                .password("password")
                .name("Idempotency User")
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        LocalDateTime start =
                LocalDateTime.of(2030, 2, 10, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2030, 2, 10, 11, 0);

        String idempotencyKey =
                "idempotency-key-" + System.nanoTime();

        Booking firstBooking = bookingService.createBooking(
                savedUser.getId(),
                savedResource.getId(),
                start,
                end,
                idempotencyKey,
                "Premier appel"
        );

        Booking secondBooking = bookingService.createBooking(
                savedUser.getId(),
                savedResource.getId(),
                start,
                end,
                idempotencyKey,
                "Deuxième appel"
        );

        assertNotNull(firstBooking);
        assertNotNull(secondBooking);

        assertEquals(
                firstBooking.getId(),
                secondBooking.getId()
        );

        long bookingCount = bookingRepository
                .findByResourceId(savedResource.getId())
                .stream()
                .filter(b ->
                        b.getStartTime().isBefore(end)
                                && b.getEndTime().isAfter(start)
                )
                .count();

        assertEquals(1, bookingCount);
    }

    @Test
    void shouldCreateOnlyOneBookingWhenSameIdempotencyKeyIsUsedConcurrently()
            throws Exception {

        Resource resource = Resource.builder()
                .name("Salle test idempotence concurrente")
                .type("ROOM")
                .description("Test de deux requêtes simultanées avec la même clé")
                .capacity(1)
                .isActive(true)
                .build();

        Resource savedResource = resourceRepository.save(resource);

        User user = User.builder()
                .email("idempotency-concurrent-" + System.nanoTime() + "@test.com")
                .password("password")
                .name("Concurrent Idempotency User")
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        LocalDateTime start =
                LocalDateTime.of(2030, 3, 10, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2030, 3, 10, 11, 0);

        String idempotencyKey =
                "same-concurrent-key-" + System.nanoTime();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startSignal =
                new CountDownLatch(1);

        Callable<Booking> booking1 = () -> {
            startSignal.await();

            return bookingService.createBooking(
                    savedUser.getId(),
                    savedResource.getId(),
                    start,
                    end,
                    idempotencyKey,
                    "Requête concurrente 1"
            );
        };

        Callable<Booking> booking2 = () -> {
            startSignal.await();

            return bookingService.createBooking(
                    savedUser.getId(),
                    savedResource.getId(),
                    start,
                    end,
                    idempotencyKey,
                    "Requête concurrente 2"
            );
        };

        Future<Booking> future1 = executor.submit(booking1);
        Future<Booking> future2 = executor.submit(booking2);

        startSignal.countDown();

        Booking result1 = null;
        Booking result2 = null;

        try {
            result1 = future1.get();
        } catch (ExecutionException e) {
            // Une requête peut échouer à cause de la concurrence.
        }

        try {
            result2 = future2.get();
        } catch (ExecutionException e) {
            // Une requête peut échouer à cause de la concurrence.
        }

        executor.shutdown();

        long bookingCount = bookingRepository
                .findByResourceId(savedResource.getId())
                .stream()
                .filter(b ->
                        b.getStartTime().isBefore(end)
                                && b.getEndTime().isAfter(start)
                )
                .count();

        assertEquals(1, bookingCount);

        if (result1 != null && result2 != null) {
            assertEquals(result1.getId(), result2.getId());
        }
    }
}
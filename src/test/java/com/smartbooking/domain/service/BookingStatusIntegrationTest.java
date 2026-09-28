package com.smartbooking.domain.service;

import com.smartbooking.domain.model.Booking;
import com.smartbooking.domain.model.BookingStatus;
import com.smartbooking.domain.model.Resource;
import com.smartbooking.domain.model.Role;
import com.smartbooking.domain.model.User;
import com.smartbooking.domain.repository.ResourceRepository;
import com.smartbooking.domain.repository.UserRepository;
import com.smartbooking.domain.service.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BookingStatusIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRejectTransitionFromCompletedToPending() {

        Resource resource = Resource.builder()
                .name("Salle test statut")
                .type("ROOM")
                .description("Ressource utilisée pour le test des statuts")
                .capacity(1)
                .isActive(true)
                .build();

        Resource savedResource = resourceRepository.save(resource);

        User user = User.builder()
                .email("status-test-" + System.nanoTime() + "@test.com")
                .password("password")
                .name("Status Test User")
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        LocalDateTime start =
                LocalDateTime.of(2030, 4, 10, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2030, 4, 10, 11, 0);

        Booking booking = bookingService.createBooking(
                savedUser.getId(),
                savedResource.getId(),
                start,
                end,
                "status-test-" + System.nanoTime(),
                "Test transition statut"
        );

        bookingService.transitionStatus(
                booking.getId(),
                BookingStatus.CONFIRMED
        );

        bookingService.transitionStatus(
                booking.getId(),
                BookingStatus.CHECKED_IN
        );

        bookingService.transitionStatus(
                booking.getId(),
                BookingStatus.COMPLETED
        );

        assertThrows(
                InvalidStatusTransitionException.class,
                () -> bookingService.transitionStatus(
                        booking.getId(),
                        BookingStatus.PENDING
                )
        );
    }
}
package com.smartbooking.api.controller;

import com.smartbooking.api.dto.BookingRequest;
import com.smartbooking.api.dto.BookingResponse;
import com.smartbooking.api.dto.BookingStatusRequest;
import com.smartbooking.config.security.UserPrincipal;
import com.smartbooking.domain.model.Booking;
import com.smartbooking.domain.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Créer une réservation.
     *
     * L'utilisateur connecté est automatiquement identifié
     * grâce au JWT.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
        @Valid @RequestBody BookingRequest request,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        Authentication authentication
    ) {

        Long userId = getCurrentUserId(authentication);

        Booking booking = bookingService.createBooking(
            userId,
            request.resourceId(),
            request.startTime(),
            request.endTime(),
            idempotencyKey,
            request.notes()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(BookingResponse.from(booking));
    }

    /**
     * Retourne les réservations de l'utilisateur connecté.
     */
    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
        Authentication authentication
    ) {

        Long userId = getCurrentUserId(authentication);

        List<BookingResponse> bookings = bookingService
            .getUserBookings(userId)
            .stream()
            .map(BookingResponse::from)
            .toList();

        return ResponseEntity.ok(bookings);
    }

    /**
     * Retourne une réservation appartenant à l'utilisateur connecté.
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
        @PathVariable Long bookingId,
        Authentication authentication
    ) {

        Long userId = getCurrentUserId(authentication);

        Booking booking = bookingService.getBooking(bookingId);

        if (!booking.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(BookingResponse.from(booking));
    }

    /**
     * Annuler une réservation.
     *
     * On ne supprime pas la réservation de la base.
     * On applique une transition vers CANCELLED.
     */
    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
        @PathVariable Long bookingId,
        Authentication authentication
    ) {

        Long userId = getCurrentUserId(authentication);

        Booking booking = bookingService.getBooking(bookingId);

        if (!booking.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Booking cancelledBooking = bookingService.transitionStatus(
            bookingId,
            com.smartbooking.domain.model.BookingStatus.CANCELLED
        );

        return ResponseEntity.ok(BookingResponse.from(cancelledBooking));
    }

    /**
     * Permet à un administrateur de faire avancer
     * une réservation dans sa machine à états.
     */
    @PatchMapping("/{bookingId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> changeStatus(
        @PathVariable Long bookingId,
        @Valid @RequestBody BookingStatusRequest request
    ) {

        Booking booking = bookingService.transitionStatus(
            bookingId,
            request.status()
        );

        return ResponseEntity.ok(BookingResponse.from(booking));
    }

    /**
     * Récupère l'identifiant de l'utilisateur connecté
     * depuis le JWT.
     */
    private Long getCurrentUserId(Authentication authentication) {

        UserPrincipal principal =
            (UserPrincipal) authentication.getPrincipal();

        return principal.getId();
    }
}
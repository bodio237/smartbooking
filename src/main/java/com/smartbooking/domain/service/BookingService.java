package com.smartbooking.domain.service;

import com.smartbooking.domain.model.Booking;
import com.smartbooking.domain.model.BookingStatus;
import com.smartbooking.domain.model.BookingTransition;
import com.smartbooking.domain.model.Resource;
import com.smartbooking.domain.model.User;
import com.smartbooking.domain.repository.BookingRepository;
import com.smartbooking.domain.repository.ResourceRepository;
import com.smartbooking.domain.repository.UserRepository;
import com.smartbooking.domain.service.exception.BookingConflictException;
import com.smartbooking.domain.service.exception.InvalidStatusTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public Booking createBooking(
            Long userId,
            Long resourceId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String idempotencyKey,
            String notes
    ) {

        // 1. Vérifier si cette requête a déjà été traitée
        var existingBooking = bookingRepository.findByIdempotencyKey(idempotencyKey);

        if (existingBooking.isPresent()) {
            return existingBooking.get();
        }

        // 2. Vérifier les dates
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "La date de fin doit être après la date de début"
            );
        }

        // 3. Récupérer l'utilisateur
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Utilisateur introuvable")
                );

        // 4. Verrouiller la ressource pendant toute la transaction
        Resource resource = resourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ressource introuvable")
                );

        // 5. Vérifier que la ressource est active
        if (!resource.getIsActive()) {
            throw new IllegalArgumentException(
                    "La ressource n'est pas active"
            );
        }

        // 6. Vérifier les chevauchements
        List<Booking> overlappingBookings =
                bookingRepository.findOverlappingBookings(
                        resourceId,
                        startTime,
                        endTime
                );

        if (!overlappingBookings.isEmpty()) {
            throw new BookingConflictException(
                    "La ressource est déjà réservée sur ce créneau"
            );
        }

        // 7. Créer la réservation
        Booking booking = new Booking();

        booking.setUser(user);
        booking.setResource(resource);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setIdempotencyKey(idempotencyKey);
        booking.setNotes(notes);
        booking.setStatus(BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking transitionStatus(
            Long bookingId,
            BookingStatus newStatus
    ) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Réservation introuvable"
                        )
                );

        BookingStatus currentStatus = booking.getStatus();

        if (!BookingTransition.isAllowed(currentStatus, newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Transition impossible : "
                            + currentStatus
                            + " → "
                            + newStatus
            );
        }

        booking.setStatus(newStatus);

        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Réservation introuvable"
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getResourceBookings(Long resourceId) {
        return bookingRepository.findByResourceId(resourceId);
    }
}
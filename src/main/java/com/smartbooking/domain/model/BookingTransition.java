package com.smartbooking.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Décrit les transitions valides entre statuts de réservation.
 * Centraliser cette logique évite d'avoir des règles éparpillées
 * dans plusieurs services.
 */
public final class BookingTransition {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED = Map.of(
        BookingStatus.PENDING,    EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED),
        BookingStatus.CONFIRMED,  EnumSet.of(BookingStatus.CHECKED_IN, BookingStatus.CANCELLED, BookingStatus.NO_SHOW),
        BookingStatus.CHECKED_IN, EnumSet.of(BookingStatus.COMPLETED),
        BookingStatus.COMPLETED,  EnumSet.noneOf(BookingStatus.class),
        BookingStatus.CANCELLED,  EnumSet.noneOf(BookingStatus.class),
        BookingStatus.NO_SHOW,    EnumSet.noneOf(BookingStatus.class)
    );

    private BookingTransition() {}

    public static boolean isAllowed(BookingStatus from, BookingStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}
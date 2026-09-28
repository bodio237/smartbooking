package com.smartbooking.domain.service;

import com.smartbooking.domain.model.BookingStatus;
import com.smartbooking.domain.model.BookingTransition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingStatusTransitionTest {

    @Test
    void shouldAllowValidTransitions() {

        assertTrue(
                BookingTransition.isAllowed(
                        BookingStatus.PENDING,
                        BookingStatus.CONFIRMED
                )
        );

        assertTrue(
                BookingTransition.isAllowed(
                        BookingStatus.CONFIRMED,
                        BookingStatus.CHECKED_IN
                )
        );

        assertTrue(
                BookingTransition.isAllowed(
                        BookingStatus.CHECKED_IN,
                        BookingStatus.COMPLETED
                )
        );

        assertTrue(
                BookingTransition.isAllowed(
                        BookingStatus.PENDING,
                        BookingStatus.CANCELLED
                )
        );
    }

    @Test
    void shouldRejectInvalidTransitions() {

        assertFalse(
                BookingTransition.isAllowed(
                        BookingStatus.COMPLETED,
                        BookingStatus.PENDING
                )
        );

        assertFalse(
                BookingTransition.isAllowed(
                        BookingStatus.CANCELLED,
                        BookingStatus.CONFIRMED
                )
        );

        assertFalse(
                BookingTransition.isAllowed(
                        BookingStatus.NO_SHOW,
                        BookingStatus.CONFIRMED
                )
        );

        assertFalse(
                BookingTransition.isAllowed(
                        BookingStatus.CHECKED_IN,
                        BookingStatus.CANCELLED
                )
        );
    }
}
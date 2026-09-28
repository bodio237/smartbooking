package com.smartbooking.api.dto;

import com.smartbooking.domain.model.Booking;
import com.smartbooking.domain.model.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(

    Long id,
    Long userId,
    Long resourceId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BookingStatus status,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt

) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
            booking.getId(),
            booking.getUser().getId(),
            booking.getResource().getId(),
            booking.getStartTime(),
            booking.getEndTime(),
            booking.getStatus(),
            booking.getNotes(),
            booking.getCreatedAt(),
            booking.getUpdatedAt()
        );
    }
}
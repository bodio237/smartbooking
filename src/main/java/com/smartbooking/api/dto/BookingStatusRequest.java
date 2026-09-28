package com.smartbooking.api.dto;

import com.smartbooking.domain.model.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record BookingStatusRequest(

    @NotNull(message = "Le nouveau statut est requis")
    BookingStatus status

) {}
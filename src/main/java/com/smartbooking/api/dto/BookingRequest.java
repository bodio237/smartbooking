package com.smartbooking.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BookingRequest(

    @NotNull(message = "La ressource est requise")
    Long resourceId,

    @NotNull(message = "La date de début est requise")
    LocalDateTime startTime,

    @NotNull(message = "La date de fin est requise")
    LocalDateTime endTime,

    @Size(max = 500, message = "Les notes ne peuvent pas dépasser 500 caractères")
    String notes

) {}
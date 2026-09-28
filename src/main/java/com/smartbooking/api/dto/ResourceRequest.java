package com.smartbooking.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResourceRequest(

        @NotBlank(message = "Le nom est requis")
        String name,

        @NotBlank(message = "Le type est requis")
        String type,

        String description,

        @NotNull(message = "La capacité est requise")
        @Min(value = 1, message = "La capacité doit être supérieure à 0")
        Integer capacity
) {
}
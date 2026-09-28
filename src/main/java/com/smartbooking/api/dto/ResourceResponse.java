package com.smartbooking.api.dto;

public record ResourceResponse(
        Long id,
        String name,
        String type,
        String description,
        Integer capacity,
        Boolean isActive
) {
}
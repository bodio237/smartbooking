package com.smartbooking.api.dto;

public record AuthResponse(
    String token,
    Long userId,
    String email,
    String name,
    String role
) {}
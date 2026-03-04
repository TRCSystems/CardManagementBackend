package com.cardsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String fullName;
    @NotBlank
    private String role;   // should match com.cardsystem.models.constants.UserRole
}

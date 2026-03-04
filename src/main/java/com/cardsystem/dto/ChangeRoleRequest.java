package com.cardsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeRoleRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String role; // must match UserRole
}

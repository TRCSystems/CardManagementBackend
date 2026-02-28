package com.cardsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BindCardRequest {
    @NotNull
    private Long studentId;
}
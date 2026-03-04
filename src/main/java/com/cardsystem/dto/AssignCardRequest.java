package com.cardsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignCardRequest {
    @NotNull
    private Long cardId;

    @NotNull
    private Long studentId;
}

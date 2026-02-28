package com.cardsystem.dto;

import com.cardsystem.models.constants.CardStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardActionResponse {
    private Long cardId;
    private String uid;
    private CardStatus status;
    private Long studentId;     // null if unassigned
    private String message;
    private boolean alreadyExists = false;
}
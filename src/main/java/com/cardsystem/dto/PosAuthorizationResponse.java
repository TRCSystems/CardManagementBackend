package com.cardsystem.dto;

import com.cardsystem.models.constants.CardStatus;
import com.cardsystem.models.constants.WalletStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PosAuthorizationResponse {
    private boolean approved;
    private String code;      // e.g., CARD_BLOCKED, PIN_INVALID, APPROVED
    private String message;
    private Long cardId;
    private CardStatus cardStatus;
    private Long walletId;
    private WalletStatus walletStatus;
}

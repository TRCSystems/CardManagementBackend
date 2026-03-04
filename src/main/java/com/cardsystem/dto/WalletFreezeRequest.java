package com.cardsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalletFreezeRequest {
    @NotNull
    private Long walletId;
    private boolean freeze = true;
}

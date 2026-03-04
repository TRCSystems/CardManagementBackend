package com.cardsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletAdjustRequest {
    @NotNull
    private Long walletId;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String reason;
}

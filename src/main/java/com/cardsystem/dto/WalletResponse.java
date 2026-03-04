package com.cardsystem.dto;

import com.cardsystem.models.constants.WalletStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WalletResponse {
    private Long walletId;
    private Long studentId;
    private String studentNumber;
    private String studentName;
    private String schoolCode;
    private BigDecimal balance;
    private WalletStatus status;
}

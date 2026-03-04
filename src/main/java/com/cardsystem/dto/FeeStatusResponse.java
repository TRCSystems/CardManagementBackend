package com.cardsystem.dto;

import com.cardsystem.models.constants.WalletStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FeeStatusResponse {
    private Long studentId;
    private String studentNumber;
    private String studentName;
    private String schoolCode;
    private BigDecimal walletBalance;
    // Placeholder: fee balance could be sourced from another module in future
    private BigDecimal feeBalance;
    private WalletStatus walletStatus;
    private LocalDateTime asOf;
}

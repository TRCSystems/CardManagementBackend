package com.cardsystem.dto;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class PaymentNotificationDto {

    @NotBlank(message = "billRefNumber is required")
    private String billRefNumber;

    @NotBlank(message = "transId is required")
    private String transId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "msisdn is required")
    private String msisdn;
}
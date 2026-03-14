package com.cardsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PosAuthorizationRequest {
    @NotBlank
    private String cardUid;

    /**
     * 4-digit numeric PIN captured on PDQ. Keep as String to preserve leading zeros.
     */
    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
    private String pin;

    // Optional purchase amount; not used yet but reserved for later balance checks.
    private String amount;
}

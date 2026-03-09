package com.cardsystem.dto;

import com.cardsystem.models.constants.CardStatus;
import com.cardsystem.models.constants.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Combined response DTO that includes Card, Student, and Wallet details.
 * Used when viewing full details of a card.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardFullDetailsResponse {
    
    // Card Details
    private Long cardId;
    private String cardUid;
    private CardStatus cardStatus;
    private LocalDateTime issuedAt;
    private LocalDateTime retiredAt;
    private String schoolCode;
    private String schoolName;
    
    // Student Details
    private Long studentId;
    private String studentNumber;
    private String studentName;
    private String classGrade;
    private LocalDateTime studentCreatedAt;
    
    // Wallet Details
    private Long walletId;
    private BigDecimal walletBalance;
    private WalletStatus walletStatus;
    private LocalDateTime walletCreatedAt;
    private LocalDateTime walletUpdatedAt;
    
    // Assignment Details
    private LocalDateTime assignedAt;
    private String assignedBy;
}

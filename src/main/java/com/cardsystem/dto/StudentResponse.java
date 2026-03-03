package com.cardsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StudentResponse {
    private Long id;
    private String studentNumber;
    private String name;
    private String classGrade;
    private String schoolCode;
    private LocalDateTime createdAt;
    // card assignment info (null if unassigned)
    private Long cardId;
    private String cardUid;
    // optional: private BigDecimal walletBalance;  // if you later add read access
}
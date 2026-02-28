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
    // optional: private BigDecimal walletBalance;  // if you later add read access
}
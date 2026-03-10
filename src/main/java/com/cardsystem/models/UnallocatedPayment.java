package com.cardsystem.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "unallocated_payments")
@Getter
@NoArgsConstructor
public class UnallocatedPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String transId;

    @Column(nullable = false, length = 100)
    private String billRefNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String msisdn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column
    private LocalDateTime resolvedAt;

    @PrePersist
    void onCreate() {
        this.receivedAt = LocalDateTime.now();
    }

    public static UnallocatedPayment of(
            String transId,
            String billRefNumber,
            BigDecimal amount,
            String msisdn,
            String reason
    ) {
        UnallocatedPayment p = new UnallocatedPayment();
        p.transId = transId;
        p.billRefNumber = billRefNumber;
        p.amount = amount;
        p.msisdn = msisdn;
        p.reason = reason;
        return p;
    }

    public void resolve() {
        this.status = Status.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    public enum Status { PENDING, RESOLVED }
}
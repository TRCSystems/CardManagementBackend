package com.cardsystem.models;

import com.cardsystem.models.constants.WalletStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallets",
        uniqueConstraints = @UniqueConstraint(columnNames = "student_id")
)
@Data
@Getter
@NoArgsConstructor
@ToString(exclude = "student")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private Student student;

    /**
     * DENORMALIZED balance.
     * Cached projection of CONFIRMED ledger transactions.
     * Never mutated directly — use debit() and credit() only.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal cachedBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE;

    /**
     * Optimistic locking — prevents double-spend under concurrency.
     */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /* =========================
       Domain guards
       ========================= */

    public void freeze() {
        this.status = WalletStatus.FROZEN;
    }

    public void activate() {
        this.status = WalletStatus.ACTIVE;
    }

    /* =========================
       Domain mutations
       ========================= */

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Debit amount must be positive");
        if (this.status != WalletStatus.ACTIVE)
            throw new IllegalStateException("Wallet is not active");
        if (this.cachedBalance.compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient balance");
        this.cachedBalance = this.cachedBalance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Credit amount must be positive");
        this.cachedBalance = this.cachedBalance.add(amount);
    }
}
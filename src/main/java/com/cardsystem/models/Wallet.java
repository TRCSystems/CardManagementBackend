package com.cardsystem.models;

import com.cardsystem.models.constants.WalletStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data                // ← generates getters, setters, toString, equals, hashCode
@Entity
@Table(
        name = "wallets",
        uniqueConstraints = @UniqueConstraint(columnNames = "student_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@ToString(exclude = "student")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Wallet is strictly bound to a student.
     * No shared wallets. No transfers between wallets.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private Student student;

    /**
     * DENORMALIZED balance.
     * This is a cached projection of CONFIRMED ledger transactions.
     * Never updated directly by controllers or POS.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal cachedBalance = BigDecimal.ZERO;

    /**
     * Controls whether the wallet can be used for debits.
     * Does NOT represent money availability.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE;

    /**
     * Optimistic locking to prevent double-spend under concurrency.
     */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /* =========================
       Lifecycle hooks
       ========================= */

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
       INTERNAL USE ONLY
       ========================= */

    /**
     * Package-private.
     * Only LedgerService is allowed to call this.
     */
    void applyLedgerDelta(BigDecimal delta) {
        this.cachedBalance = this.cachedBalance.add(delta);
    }
}

package com.cardsystem.models;

import com.cardsystem.models.constants.TransactionSource;
import com.cardsystem.models.constants.TransactionStatus;
import com.cardsystem.models.constants.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wallet_tx_wallet", columnList = "wallet_id"),
                @Index(name = "idx_wallet_tx_reference", columnList = "externalReference"),
                @Index(name = "idx_wallet_tx_created", columnList = "createdAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_wallet_tx_source_reference",
                        columnNames = {"source", "externalReference"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "wallet")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Wallet affected by this transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    /**
     * CREDIT or DEBIT.
     * Direction relative to the wallet.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TransactionType type;

    /**
     * Where this transaction originated from.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private TransactionSource source;

    /**
     * Amount of the transaction.
     * Always positive.
     */
    @Column(nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    /**
     * External idempotency key.
     * Examples:
     * - M-Pesa receipt number
     * - POS transaction UUID
     */
    @Column(nullable = false, length = 100, updatable = false)
    private String externalReference;

    /**
     * Transaction lifecycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /**
     * Optional pointer to another transaction
     * (used for reversals).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_transaction_id")
    private WalletTransaction reversedTransaction;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime confirmedAt;

    /* =========================
       Lifecycle hooks
       ========================= */

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
    }

    /* =========================
       Domain invariants
       ========================= */

    public void confirm() {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING transactions can be confirmed");
        }
        this.status = TransactionStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void markFailed() {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING transactions can fail");
        }
        this.status = TransactionStatus.FAILED;
    }
}

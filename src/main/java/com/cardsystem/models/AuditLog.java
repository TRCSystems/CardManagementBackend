package com.cardsystem.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for tracking all system audit logs.
 * Records every significant action performed in the system.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_actor", columnList = "actor"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity_type", columnList = "entityType"),
    @Index(name = "idx_audit_entity_id", columnList = "entityId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Timestamp when the action occurred
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * The user who performed the action (username)
     */
    @Column(length = 100, nullable = false)
    private String actor;

    /**
     * The role of the actor at the time of action
     */
    @Column(length = 30)
    private String actorRole;

    /**
     * The action performed (e.g., "CARD_ISSUED", "WALLET_ADJUSTED", "STUDENT_CREATED")
     */
    @Column(length = 50, nullable = false)
    private String action;

    /**
     * Category of the action (CARD, WALLET, STUDENT, AUTH, USER)
     */
    @Column(length = 30, nullable = false)
    private String category;

    /**
     * Type of entity affected (e.g., "Card", "Wallet", "Student", "User")
     */
    @Column(length = 50)
    private String entityType;

    /**
     * ID of the affected entity
     */
    @Column
    private Long entityId;

    /**
     * Detailed description of what happened
     */
    @Column(length = 1000)
    private String details;

    /**
     * Previous state (JSON representation of state before change)
     */
    @Column(columnDefinition = "TEXT")
    private String previousState;

    /**
     * New state (JSON representation of state after change)
     */
    @Column(columnDefinition = "TEXT")
    private String newState;

    /**
     * IP address of the client (if available)
     */
    @Column(length = 45)
    private String ipAddress;

    /**
     * Whether the action was successful
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    /**
     * Error message if action failed
     */
    @Column(length = 500)
    private String errorMessage;

    /**
     * Additional metadata as JSON (for extra context)
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

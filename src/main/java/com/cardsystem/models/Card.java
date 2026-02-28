package com.cardsystem.models;

import com.cardsystem.models.constants.CardStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@ToString(exclude = "school")
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String uid;                // uppercase hex, e.g. "04AABBCCDD112233"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status = CardStatus.UNASSIGNED;

    @Column(updatable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Column
    private LocalDateTime retiredAt;

    // Denormalized pointer – updated when assignment changes (for fast POS lookup)
    @Column(name = "current_assignment_id")
    private Long currentAssignmentId;

    public boolean isActiveForSpending() {
        return status == CardStatus.ASSIGNED && currentAssignmentId != null;
    }
}


package com.cardsystem.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_assignments")
@Getter
@Setter
@ToString(exclude = {"card", "student", "assignedBy", "unassignedBy"})
@NoArgsConstructor
@AllArgsConstructor
public class CardAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Student student;

    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column
    private LocalDateTime unassignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;           // → we'll define User later

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unassigned_by")
    private User unassignedBy;

    @Column(length = 200)
    private String reason;             // "Lost card", "Replaced", "Student transferred"
}
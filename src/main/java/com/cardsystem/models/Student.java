package com.cardsystem.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "students",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_school_number",
                columnNames = {"school_id", "student_number"}
        ))
@Getter
@Setter
@ToString(exclude = {"school", "wallet"})
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "student_number", length = 30, nullable = false)
    private String studentNumber;   // e.g. "ADM00123", "2025-001"

    @Column(nullable = false)
    private String name;

    @Column(name = "class_grade", length = 50)
    private String classGrade;

    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private Wallet wallet;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    // Later: parent contact, photo reference, status (active/left), etc.
}
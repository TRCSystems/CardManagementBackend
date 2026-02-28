package com.cardsystem.models;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "schools")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class School {

    @Id
    @Column(name = "code", length = 20)
    private String code;           // e.g. "SCH001", "NAI-001" – natural/business key

    @Column(nullable = false)
    private String name;

    @Column
    private String contactEmail;

    @Column
    private String contactPhone;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    // You can add later: address, trustAccountDetails, etc.
}
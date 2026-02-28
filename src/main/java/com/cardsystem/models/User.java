package com.cardsystem.models;


import com.cardsystem.models.constants.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;       // or email

    // For now – do NOT store plain passwords in production!
    // Later: use BCryptPasswordEncoder
    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private UserRole role = UserRole.OPERATOR;  // default

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @Column
    private boolean active = true;
}


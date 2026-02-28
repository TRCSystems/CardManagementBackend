//package com.cardsystem.auth.models;
//
//
//import com.cardsystem.models.constants.UserRole;
//import com.cardsystem.models.constants.Status;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@AllArgsConstructor
//@NoArgsConstructor
//@Data
//@Setter
//@Getter
//@Builder
//public class User {
//
//    @Id
//    @GeneratedValue( strategy = GenerationType.IDENTITY )
//    private Long userId;
//    @Column(nullable = false)
//    private String username;
//    @Column(nullable = false)
//    private String password;
//
//    @Enumerated(EnumType.STRING)
//    private UserRole role;
//
//    @Enumerated(EnumType.STRING)
//    private Status status;
//
//
//}

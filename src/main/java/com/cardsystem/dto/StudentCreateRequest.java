package com.cardsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentCreateRequest {
    @NotBlank(message = "Admission/Student number is required")
    @Size(max = 30)
    private String studentNumber;

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 50)
    private String classGrade;

    // Optional fields you can add later
    // private String parentPhone;
    // private String parentEmail;
}
package com.cardsystem.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentUpdateRequest {
    @Size(max = 150)
    private String name;

    @Size(max = 50)
    private String classGrade;

    // Add more fields as needed
}
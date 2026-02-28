package com.cardsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class IssueCardRequest {

    @NotBlank(message = "UID cannot be blank")
    @Size(min = 8, max = 32, message = "UID length must be between 8 and 32 characters")
    @Pattern(
            regexp = "^[0-9A-Fa-f]+$",   // ← this is the correct attribute name: regexp
            message = "UID must contain only hexadecimal characters (0-9, A-F, a-f)"
    )
    private String uid;

    @NotBlank(message = "School ID is required")
    private String schoolId;
}
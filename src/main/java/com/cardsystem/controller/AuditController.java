package com.cardsystem.controller;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','FINANCE_ADMIN')")
    public ResponseEntity<List<AuditLogEntry>> getLogs() {
        // Placeholder: no persistence yet; return empty list
        return ResponseEntity.ok(Collections.emptyList());
    }

    @Data
    @Builder
    public static class AuditLogEntry {
        private LocalDateTime timestamp;
        private String actor;
        private String action;
        private String details;
    }
}

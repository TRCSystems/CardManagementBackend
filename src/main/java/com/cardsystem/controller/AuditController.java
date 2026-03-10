package com.cardsystem.controller;

import com.cardsystem.models.AuditLog;
import com.cardsystem.services.AuditService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for retrieving audit logs and statistics.
 * Provides comprehensive reporting of all system activities.
 */
@RestController
@RequestMapping("/api/v1/audit")
@AllArgsConstructor
public class AuditController {

    private final AuditService auditService;

    //1. Get all audit logs including failed ones (paginated)  //
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','FINANCE_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AuditLog> logs = auditService.getAuditLogs(pageable);
        Page<AuditLogResponse> response = logs.map(this::toResponse);
        return ResponseEntity.ok(response);
    }

    //2.Get audit logs within a date range (for filtering by date)*//

    @GetMapping("/logs/by-date")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','FINANCE_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> logs = auditService.getLogsByDateRange(startDate, endDate, pageable);
        Page<AuditLogResponse> response = logs.map(this::toResponse);
        return ResponseEntity.ok(response);
    }

    //3. Get failed operations only //

    @GetMapping("/logs/failed")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getFailedOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> logs = auditService.getFailedOperations(pageable);
        Page<AuditLogResponse> response = logs.map(this::toResponse);
        return ResponseEntity.ok(response);
    }

    // ==================== Response DTO ====================

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .timestamp(log.getTimestamp())
                .actor(log.getActor())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .category(log.getCategory())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .success(log.isSuccess())
                .errorMessage(log.getErrorMessage())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogResponse {
        private Long id;
        private LocalDateTime timestamp;
        private String actor;
        private String actorRole;
        private String action;
        private String category;
        private String entityType;
        private Long entityId;
        private String details;
        private boolean success;
        private String errorMessage;
    }
}

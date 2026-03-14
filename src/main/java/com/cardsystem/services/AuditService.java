package com.cardsystem.services;

import com.cardsystem.models.AuditLog;
import com.cardsystem.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for recording and retrieving audit logs.
 * Provides comprehensive tracking of all system operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // ==================== Constants for Actions ====================
    
    // Card Actions
    public static final String ACTION_CARD_ISSUED = "CARD_ISSUED";
    public static final String ACTION_CARD_BOUND = "CARD_BOUND";
    public static final String ACTION_CARD_BLOCKED = "CARD_BLOCKED";
    public static final String ACTION_CARD_UNBLOCKED = "CARD_UNBLOCKED";
    public static final String ACTION_CARD_RETIRED = "CARD_RETIRED";
    public static final String ACTION_CARD_PIN_SET = "CARD_PIN_SET";
    public static final String ACTION_CARD_PIN_FAILED = "CARD_PIN_FAILED";
    public static final String ACTION_CARD_PIN_LOCKED = "CARD_PIN_LOCKED";
    public static final String ACTION_CARD_LISTED = "CARD_LISTED";
    
    // Wallet Actions
    public static final String ACTION_WALLET_CREDIT = "WALLET_CREDIT";
    public static final String ACTION_WALLET_DEBIT = "WALLET_DEBIT";
    public static final String ACTION_WALLET_ADJUSTED = "WALLET_ADJUSTED";
    public static final String ACTION_WALLET_FROZEN = "WALLET_FROZEN";
    public static final String ACTION_WALLET_UNFROZEN = "WALLET_UNFROZEN";
    public static final String ACTION_WALLET_MPESA_C2B = "WALLET_MPESA_C2B";
    public static final String ACTION_WALLET_BALANCE_CHECKED = "WALLET_BALANCE_CHECKED";
    
    // Student Actions
    public static final String ACTION_STUDENT_CREATED = "STUDENT_CREATED";
    public static final String ACTION_STUDENT_UPDATED = "STUDENT_UPDATED";
    public static final String ACTION_STUDENT_BULK_UPLOAD = "STUDENT_BULK_UPLOAD";
    public static final String ACTION_STUDENT_LISTED = "STUDENT_LISTED";
    public static final String ACTION_STUDENT_FEE_STATUS = "STUDENT_FEE_STATUS";
    
    // Auth Actions
    public static final String ACTION_USER_LOGIN = "USER_LOGIN";
    public static final String ACTION_USER_LOGOUT = "USER_LOGOUT";
    public static final String ACTION_USER_REGISTERED = "USER_REGISTERED";
    public static final String ACTION_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String ACTION_ROLE_CHANGED = "ROLE_CHANGED";
    public static final String ACTION_TOKEN_REFRESHED = "TOKEN_REFRESHED";
    
    // Categories
    public static final String CATEGORY_CARD = "CARD";
    public static final String CATEGORY_WALLET = "WALLET";
    public static final String CATEGORY_STUDENT = "STUDENT";
    public static final String CATEGORY_AUTH = "AUTH";
    public static final String CATEGORY_USER = "USER";
    
    // Entity Types
    public static final String ENTITY_CARD = "Card";
    public static final String ENTITY_WALLET = "Wallet";
    public static final String ENTITY_STUDENT = "Student";
    public static final String ENTITY_USER = "User";

    // ==================== Log Creation Methods ====================

    /**
     * Log an action with basic information
     */
    public AuditLog logAction(String action, String category, String details) {
        return createAuditLog(action, category, null, null, details, null, null, null, true, null, null);
    }

    /**
     * Log an action with entity information
     */
    public AuditLog logAction(String action, String category, String entityType, Long entityId, String details) {
        return createAuditLog(action, category, entityType, entityId, details, null, null, null, true, null, null);
    }

    /**
     * Log an action with full details including state changes
     */
    public AuditLog logAction(String action, String category, String entityType, Long entityId, 
                              String details, String previousState, String newState) {
        return createAuditLog(action, category, entityType, entityId, details, previousState, newState, 
                            null, true, null, null);
    }

    /**
     * Log a successful action with all details
     */
    public AuditLog logAction(String action, String category, String entityType, Long entityId, 
                              String details, String previousState, String newState, String metadata) {
        return createAuditLog(action, category, entityType, entityId, details, previousState, newState, 
                            metadata, true, null, null);
    }

    /**
     * Log a failed action
     */
    public AuditLog logFailure(String action, String category, String details, String errorMessage) {
        return createAuditLog(action, category, null, null, details, null, null, null, false, errorMessage, null);
    }

    /**
     * Log an action with IP address
     */
    public AuditLog logActionWithIp(String action, String category, String entityType, Long entityId,
                                     String details, String ipAddress) {
        return createAuditLog(action, category, entityType, entityId, details, null, null, null, 
                            true, null, ipAddress);
    }

    // ==================== Private Helper Methods ====================

    private AuditLog createAuditLog(String action, String category, String entityType, Long entityId,
                                     String details, String previousState, String newState, 
                                     String metadata, boolean success, String errorMessage, String ipAddress) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = "SYSTEM";
        String actorRole = null;
        
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            actor = auth.getName();
            actorRole = auth.getAuthorities().stream()
                    .map(Object::toString)
                    .findFirst()
                    .orElse(null);
        }

        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .actor(actor)
                .actorRole(actorRole)
                .action(action)
                .category(category)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .previousState(previousState)
                .newState(newState)
                .metadata(metadata)
                .success(success)
                .errorMessage(errorMessage)
                .ipAddress(ipAddress)
                .build();

        try {
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== Query Methods ====================

    /**
     * Get paginated audit logs
     */
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    /**
     * Get audit logs for a specific entity
     */
    public List<AuditLog> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findEntityHistory(entityType, entityId);
    }

    /**
     * Search audit logs with filters
     */
    public Page<AuditLog> searchAuditLogs(String actor, String action, String category, 
                                           String entityType, Long entityId,
                                           LocalDateTime startDate, LocalDateTime endDate,
                                           Boolean success, Pageable pageable) {
        return auditLogRepository.searchAuditLogs(actor, action, category, entityType, entityId,
                startDate, endDate, success, pageable);
    }

    /**
     * Get all logs for a specific actor
     */
    public Page<AuditLog> getLogsByActor(String actor, Pageable pageable) {
        return auditLogRepository.findByActorOrderByTimestampDesc(actor, pageable);
    }

    /**
     * Get all logs for a specific action type
     */
    public Page<AuditLog> getLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action, pageable);
    }

    /**
     * Get all logs for a specific category
     */
    public Page<AuditLog> getLogsByCategory(String category, Pageable pageable) {
        return auditLogRepository.findByCategoryOrderByTimestampDesc(category, pageable);
    }

    /**
     * Get logs within a date range
     */
    public Page<AuditLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
    }

    /**
     * Get failed operations
     */
    public Page<AuditLog> getFailedOperations(Pageable pageable) {
        return auditLogRepository.findBySuccessFalseOrderByTimestampDesc(pageable);
    }

    /**
     * Get action statistics for a date range
     */
    public List<Object[]> getActionStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.countActionsByType(startDate, endDate);
    }

    /**
     * Get category statistics for a date range
     */
    public List<Object[]> getCategoryStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.countActionsByCategory(startDate, endDate);
    }

    /**
     * Get a specific audit log by ID
     */
    public Optional<AuditLog> getAuditLogById(Long id) {
        return auditLogRepository.findById(id);
    }

    // ==================== Utility Methods ====================

    /**
     * Get current user info for audit purposes
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    /**
     * Check if current user has a specific role
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
        }
        return false;
    }

    /**
     * Get current user role
     */
    public String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .map(Object::toString)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}

package com.cardsystem.repository;

import com.cardsystem.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity with advanced filtering capabilities.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific entity
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);

    /**
     * Find all audit logs by actor (username)
     */
    Page<AuditLog> findByActorOrderByTimestampDesc(String actor, Pageable pageable);

    /**
     * Find all audit logs by action
     */
    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    /**
     * Find all audit logs by category
     */
    Page<AuditLog> findByCategoryOrderByTimestampDesc(String category, Pageable pageable);

    /**
     * Find all audit logs within a date range
     */
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find all audit logs for a specific entity type
     */
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    /**
     * Advanced search with multiple filters
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actor IS NULL OR a.actor = :actor) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:category IS NULL OR a.category = :category) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) AND " +
           "(:success IS NULL OR a.success = :success) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("actor") String actor,
            @Param("action") String action,
            @Param("category") String category,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("success") Boolean success,
            Pageable pageable);

    /**
     * Get count of actions by type for a date range
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate GROUP BY a.action")
    List<Object[]> countActionsByType(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get count of actions by category for a date range
     */
    @Query("SELECT a.category, COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate GROUP BY a.category")
    List<Object[]> countActionsByCategory(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find failed actions
     */
    Page<AuditLog> findBySuccessFalseOrderByTimestampDesc(Pageable pageable);

    /**
     * Find recent actions by multiple actors
     */
    List<AuditLog> findByActorInOrderByTimestampDesc(List<String> actors);

    /**
     * Find all actions on a specific entity ordered by timestamp
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<AuditLog> findEntityHistory(@Param("entityType") String entityType, @Param("entityId") Long entityId);
}

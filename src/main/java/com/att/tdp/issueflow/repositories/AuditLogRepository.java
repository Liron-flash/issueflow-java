package com.att.tdp.issueflow.repositories;

import com.att.tdp.issueflow.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT log
            FROM AuditLog log
            WHERE (:entityType IS NULL OR log.entityType = :entityType)
              AND (:entityId IS NULL OR log.entityId = :entityId)
              AND (:action IS NULL OR log.action = :action)
              AND (:actor IS NULL OR log.actor = :actor)
            ORDER BY log.timestamp DESC
            """)
    List<AuditLog> search(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("action") String action,
            @Param("actor") String actor
    );
}
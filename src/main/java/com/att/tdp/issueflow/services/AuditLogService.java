package com.att.tdp.issueflow.services;

import com.att.tdp.issueflow.entities.AuditLog;
import com.att.tdp.issueflow.entities.User;
import com.att.tdp.issueflow.repositories.AuditLogRepository;
import com.att.tdp.issueflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final String ACTOR_USER = "USER";
    private static final String ACTOR_SYSTEM = "SYSTEM";

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void logUserAction(String action, String entityType, Long entityId) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(resolveCurrentUserId())
                .actor(ACTOR_USER)
                .build();

        auditLogRepository.save(auditLog);
    }

    public void logSystemAction(String action, String entityType, Long entityId) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(null)
                .actor(ACTOR_SYSTEM)
                .build();

        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> searchLogs(String entityType, Long entityId, String action, String actor) {
        return auditLogRepository.search(entityType, entityId, action, actor);
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();

        if (username == null || username.equals("anonymousUser")) {
            return null;
        }

        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElse(null);
    }
}
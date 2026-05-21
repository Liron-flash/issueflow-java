package com.att.tdp.issueflow.controllers;

import com.att.tdp.issueflow.entities.AuditLog;
import com.att.tdp.issueflow.services.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor
    ) {
        return ResponseEntity.ok(
                auditLogService.searchLogs(entityType, entityId, action, actor)
        );
    }
}
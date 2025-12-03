package com.crypto.controller;

import com.crypto.service.TradingBotAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final TradingBotAuditService auditService;

    @GetMapping("/my-logs")
    public ResponseEntity<?> getMyAuditLogs(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String username = auth.getName();
        var logs = auditService.getUserAuditLogs(username, page, size);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/critical")
    public ResponseEntity<?> getMyCriticalLogs(Authentication auth) {
        String username = auth.getName();
        var logs = auditService.getCriticalLogs(username);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/admin/recent-critical")
    public ResponseEntity<?> getSystemCriticalLogs(
            @RequestParam(defaultValue = "24") int hours) {

        var logs = auditService.getRecentCriticalSystemLogs(hours);

        return ResponseEntity.ok(Map.of(
                "criticalEvents", logs,
                "count", logs.size(),
                "timeRange", hours + " hours"
        ));
    }
}
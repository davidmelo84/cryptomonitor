package com.crypto.controller;

import com.crypto.service.UserCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/admin/cleanup")
@RequiredArgsConstructor
public class AdminCleanupController {

    private final UserCleanupService cleanupService;


    @PostMapping("/run")
    public ResponseEntity<?> runCleanup() {
        try {
            log.info("🧹 Executando limpeza manual...");

            Map<String, Object> result = cleanupService.performManualCleanup();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erro na limpeza: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }


    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Map<String, Object> stats = cleanupService.getUnverifiedStats();

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Erro ao obter stats: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/tokens")
    public ResponseEntity<?> cleanupTokens() {
        try {
            int removed = cleanupService.cleanupExpiredTokens();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tokensRemoved", removed,
                    "message", removed > 0
                            ? removed + " tokens removidos"
                            : "Nenhum token expirado encontrado"
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao limpar tokens: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/accounts")
    public ResponseEntity<?> cleanupAccounts() {
        try {
            int removed = cleanupService.cleanupOldUnverifiedUsers();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "accountsRemoved", removed,
                    "message", removed > 0
                            ? removed + " contas removidas"
                            : "Nenhuma conta antiga não verificada encontrada"
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao limpar contas: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            Map<String, Object> stats = cleanupService.getUnverifiedStats();

            long unverified = (Long) stats.get("unverifiedUsers");
            long oldUnverified = (Long) stats.get("oldUnverified");

            String status = oldUnverified > 10 ? "WARNING" : "HEALTHY";
            String message = oldUnverified > 10
                    ? "Existem " + oldUnverified + " contas antigas não verificadas"
                    : "Sistema de limpeza operando normalmente";

            return ResponseEntity.ok(Map.of(
                    "status", status,
                    "message", message,
                    "stats", stats
            ));

        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "status", "DOWN",
                            "error", e.getMessage()
                    ));
        }
    }
}
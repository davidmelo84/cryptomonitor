package com.crypto.controller;

import com.crypto.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ✅ SPRINT 2 - Endpoint para testar WebSocket
 */
@Slf4j
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
public class WebSocketTestController {

    private final WebSocketService webSocketService;

    /**
     * Enviar mensagem de teste via WebSocket
     */
    @PostMapping("/test")
    public ResponseEntity<?> testWebSocket(@RequestBody Map<String, Object> message) {
        try {
            log.info("🧪 Enviando mensagem de teste via WebSocket");

            webSocketService.broadcastSystemStatus(Map.of(
                    "type", "test",
                    "message", message.getOrDefault("message", "Test message"),
                    "timestamp", System.currentTimeMillis()
            ));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mensagem enviada via WebSocket"
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao testar WebSocket: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
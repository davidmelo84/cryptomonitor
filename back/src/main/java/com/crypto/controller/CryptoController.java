// Localização: back/src/main/java/com/crypto/controller/CryptoController.java
package com.crypto.controller;

import com.crypto.dto.CryptoCurrency;
import com.crypto.service.CryptoService;
import com.crypto.util.InputSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * ✅ CRYPTO CONTROLLER - PROTEGIDO CONTRA RATE LIMIT + SANITIZAÇÃO
 *
 * Recursos:
 * - Cache em todos endpoints
 * - Browser Cache-Control
 * - Sanitização de coinId
 * - Proteção contra inputs inválidos
 */
@Slf4j
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoService cryptoService;

    // ✅ Sanitização adicionada
    private final InputSanitizer sanitizer;

    /**
     * ✅ BUSCAR PREÇOS ATUAIS
     *
     * Cache:
     * - Backend: 30min (via Service)
     * - Browser: 5min
     */
    @GetMapping("/current")
    public ResponseEntity<List<CryptoCurrency>> getCurrentPrices() {
        try {
            log.debug("📊 Endpoint /current chamado");

            List<CryptoCurrency> cryptos = cryptoService.getCurrentPrices();

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                    .body(cryptos);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar preços: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ✅ BUSCAR UMA MOEDA ESPECÍFICA
     *
     * Agora com sanitização da coinId
     */
    @GetMapping("/current/{coinId}")
    public ResponseEntity<CryptoCurrency> getCryptoByCoinId(@PathVariable String coinId) {
        try {
            // 🔒 Sanitização
            coinId = sanitizer.sanitizeCoinId(coinId);

            log.debug("🔍 Buscando: {}", coinId);

            Optional<CryptoCurrency> crypto = cryptoService.getCryptoByCoinId(coinId);

            return crypto
                    .map(c -> ResponseEntity.ok()
                            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                            .body(c))
                    .orElse(ResponseEntity.notFound().build());

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ CoinId inválido: {}", coinId);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Erro ao buscar {}: {}", coinId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ✅ BUSCAR HISTÓRICO DE PREÇOS (para gráficos)
     *
     * Cache backend: 2h
     */
    @GetMapping("/history/{coinId}")
    public ResponseEntity<Map<String, Object>> getCryptoHistory(
            @PathVariable String coinId,
            @RequestParam(defaultValue = "7") int days
    ) {
        try {
            // 🔒 Sanitização
            coinId = sanitizer.sanitizeCoinId(coinId);

            // Validar dias
            if (days < 1 || days > 365) {
                log.warn("⚠️ Valor inválido para days: {}", days);
                return ResponseEntity.badRequest().build();
            }

            log.debug("📈 Buscando histórico: {} ({}d)", coinId, days);

            List<Map<String, Object>> history = cryptoService.getHistory(coinId, days);

            if (history.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = Map.of(
                    "coinId", coinId,
                    "days", days,
                    "data", history,
                    "cached", true
            );

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(2, TimeUnit.HOURS))
                    .body(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Input inválido: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Erro ao buscar histórico: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ⚠️ FORÇAR ATUALIZAÇÃO (ADMIN SOMENTE)
     */
    @PostMapping("/force-update")
    public ResponseEntity<Map<String, Object>> forceUpdate() {
        try {
            log.warn("⚠️ FORCE UPDATE solicitado!");

            cryptoService.clearCache();
            List<CryptoCurrency> cryptos = cryptoService.getCurrentPrices();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache limpo e dados atualizados",
                    "count", cryptos.size(),
                    "warning", "Use este endpoint com moderação!"
            ));

        } catch (Exception e) {
            log.error("❌ Erro no force update: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ STATUS DA API
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getApiStatus() {
        Map<String, Object> status = cryptoService.getApiStatus();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES))
                .body(status);
    }
}

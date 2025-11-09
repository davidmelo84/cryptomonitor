// back/src/main/java/com/crypto/controller/CoinCapTestController.java
package com.crypto.controller;

import com.crypto.dto.CryptoCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ✅ CONTROLLER DE TESTE - COINCAP API
 *
 * Use estes endpoints para validar a integração
 */
@Slf4j
@RestController
@RequestMapping("/api/test/coincap")
@RequiredArgsConstructor
public class CoinCapTestController {

    private final CoinCapApiService coinCapService;

    /**
     * ✅ Health Check
     * GET /api/test/coincap/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        boolean available = coinCapService.isAvailable();

        return ResponseEntity.ok(Map.of(
                "provider", "CoinCap",
                "status", available ? "OPERATIONAL" : "DOWN",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * ✅ Testar busca de todas as moedas
     * GET /api/test/coincap/all
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllPrices() {
        try {
            log.info("🧪 Testando getAllPrices()...");

            List<CryptoCurrency> prices = coinCapService.getAllPrices();

            if (prices.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Nenhuma moeda retornada",
                        "count", 0
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", prices.size(),
                    "prices", prices
            ));

        } catch (Exception e) {
            log.error("❌ Erro no teste: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ✅ Testar busca de UMA moeda
     * GET /api/test/coincap/coin/{coinId}
     *
     * Exemplos:
     * - /api/test/coincap/coin/bitcoin
     * - /api/test/coincap/coin/ethereum
     */
    @GetMapping("/coin/{coinId}")
    public ResponseEntity<?> getCoin(@PathVariable String coinId) {
        try {
            log.info("🧪 Testando getPrice({})...", coinId);

            Optional<CryptoCurrency> crypto = coinCapService.getPrice(coinId);

            if (crypto.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Moeda não encontrada",
                        "coinId", coinId
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "crypto", crypto.get()
            ));

        } catch (Exception e) {
            log.error("❌ Erro no teste: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ✅ Testar Top N moedas
     * GET /api/test/coincap/top?limit=5
     */
    @GetMapping("/top")
    public ResponseEntity<?> getTop(@RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("🧪 Testando getTopPrices({})...", limit);

            List<CryptoCurrency> prices = coinCapService.getTopPrices(limit);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", prices.size(),
                    "limit", limit,
                    "prices", prices
            ));

        } catch (Exception e) {
            log.error("❌ Erro no teste: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ✅ Informações sobre rate limiting
     * GET /api/test/coincap/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        Map<String, Object> info = coinCapService.getRateLimitInfo();

        info.put("supportedCoins", coinCapService.getSupportedCoins());
        info.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(info);
    }

    /**
     * ✅ Testar histórico (para gráficos)
     * GET /api/test/coincap/history/{coinId}?interval=h1
     *
     * Intervalos: m1, m5, m15, m30, h1, h2, h6, h12, d1
     */
    @GetMapping("/history/{coinId}")
    public ResponseEntity<?> getHistory(
            @PathVariable String coinId,
            @RequestParam(defaultValue = "h1") String interval
    ) {
        try {
            log.info("🧪 Testando getHistory({}, {})...", coinId, interval);

            List<Map<String, Object>> history = coinCapService.getHistory(coinId, interval);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "coinId", coinId,
                    "interval", interval,
                    "dataPoints", history.size(),
                    "history", history
            ));

        } catch (Exception e) {
            log.error("❌ Erro no teste: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
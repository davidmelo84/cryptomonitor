// back/src/main/java/com/crypto/controller/CryptoController.java
package com.crypto.controller;

import com.crypto.dto.CryptoCurrency;
import com.crypto.service.CryptoService;
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
 * ✅ CRYPTO CONTROLLER - PROTEGIDO CONTRA RATE LIMIT
 *
 * REGRAS:
 * - TODOS os endpoints SEMPRE usam cache
 * - NUNCA chamam API diretamente
 * - Cache-Control headers para browser cache
 * - Rate limit no nível do controller
 */
@Slf4j
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoService cryptoService;

    /**
     * ✅ BUSCAR PREÇOS ATUAIS
     *
     * PROTEÇÕES:
     * - Cache 30min no backend
     * - Cache-Control 5min no browser
     * - NUNCA bypassa cache
     */
    @GetMapping("/current")
    public ResponseEntity<List<CryptoCurrency>> getCurrentPrices() {
        try {
            log.debug("📊 Endpoint /current chamado");

            // ✅ SEMPRE usa cache
            List<CryptoCurrency> cryptos = cryptoService.getCurrentPrices();

            // ✅ Adicionar cache no browser (5 minutos)
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
     */
    @GetMapping("/current/{coinId}")
    public ResponseEntity<CryptoCurrency> getCryptoByCoinId(@PathVariable String coinId) {
        try {
            log.debug("🔍 Buscando: {}", coinId);

            Optional<CryptoCurrency> crypto = cryptoService.getCryptoByCoinId(coinId);

            return crypto
                    .map(c -> ResponseEntity.ok()
                            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                            .body(c))
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("❌ Erro ao buscar {}: {}", coinId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ✅ BUSCAR HISTÓRICO (para gráficos)
     *
     * Cache 2 horas (histórico muda menos)
     */
    @GetMapping("/history/{coinId}")
    public ResponseEntity<Map<String, Object>> getCryptoHistory(
            @PathVariable String coinId,
            @RequestParam(defaultValue = "7") int days
    ) {
        try {
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

        } catch (Exception e) {
            log.error("❌ Erro ao buscar histórico: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ⚠️ FORÇAR ATUALIZAÇÃO (ADMIN APENAS!)
     *
     * Use com CUIDADO - consome rate limit!
     */
    @PostMapping("/force-update")
    public ResponseEntity<Map<String, Object>> forceUpdate() {
        try {
            log.warn("⚠️ FORCE UPDATE solicitado!");

            // ✅ Limpar cache e buscar novos dados
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
     * ✅ STATUS DA API (sem consumir rate limit)
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getApiStatus() {
        Map<String, Object> status = cryptoService.getApiStatus();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES))
                .body(status);
    }

    /**
     * ❌ REMOVIDO: /update endpoint
     *
     * Motivo: Permitia bypass do cache
     * Use o scheduler automático ao invés!
     */
}
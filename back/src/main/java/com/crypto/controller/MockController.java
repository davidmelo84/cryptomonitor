// back/src/main/java/com/crypto/controller/MockController.java

package com.crypto.controller;

import com.crypto.service.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para gerenciar o sistema de mock de criptomoedas
 * Útil para testes e desenvolvimento
 */
@Slf4j
@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
public class MockController {

    private final CryptoService cryptoService;

    /**
     * Listar todas as moedas disponíveis no mock
     */
    @GetMapping("/coins")
    public ResponseEntity<?> getAvailableMockCoins() {
        try {
            Map<String, String> coins = cryptoService.getAvailableMockCoins();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "coins", coins,
                    "total", coins.size()
            ));

        } catch (Exception e) {
            log.error("Erro ao listar moedas mock: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Resetar preço de uma moeda específica
     */
    @PostMapping("/reset/{coinId}")
    public ResponseEntity<?> resetCoinPrice(@PathVariable String coinId) {
        try {
            cryptoService.resetMockPrice(coinId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Preço de " + coinId + " resetado com sucesso"
            ));

        } catch (Exception e) {
            log.error("Erro ao resetar preço: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Resetar preços de TODAS as moedas
     */
    @PostMapping("/reset-all")
    public ResponseEntity<?> resetAllPrices() {
        try {
            cryptoService.resetAllMockPrices();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Todos os preços foram resetados"
            ));

        } catch (Exception e) {
            log.error("Erro ao resetar todos os preços: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Obter status do sistema de mock
     */
    @GetMapping("/status")
    public ResponseEntity<?> getMockStatus() {
        try {
            Map<String, String> coins = cryptoService.getAvailableMockCoins();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "enabled", true,
                    "totalCoins", coins.size(),
                    "coins", coins
            ));

        } catch (Exception e) {
            log.error("Erro ao obter status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
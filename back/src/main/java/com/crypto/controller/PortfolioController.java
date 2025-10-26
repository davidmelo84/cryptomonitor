// back/src/main/java/com/crypto/controller/PortfolioController.java

package com.crypto.controller;

import com.crypto.model.Transaction;
import com.crypto.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Busca portfolio do usuário autenticado
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPortfolio(Authentication authentication) {
        try {
            String username = authentication.getName();
            Map<String, Object> portfolio = portfolioService.getPortfolio(username);
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            log.error("Erro ao buscar portfolio: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao buscar portfolio: " + e.getMessage()));
        }
    }

    /**
     * Adiciona uma transação
     */
    @PostMapping("/transaction")
    public ResponseEntity<?> addTransaction(
            @RequestBody Transaction transaction,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            Transaction savedTransaction = portfolioService.addTransaction(username, transaction);
            return ResponseEntity.ok(savedTransaction);
        } catch (Exception e) {
            log.error("Erro ao adicionar transação: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Busca histórico de transações
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<Transaction> transactions = portfolioService.getTransactions(username);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Erro ao buscar transações: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Deleta uma transação
     */
    @DeleteMapping("/transaction/{id}")
    public ResponseEntity<?> deleteTransaction(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            portfolioService.deleteTransaction(username, id);
            return ResponseEntity.ok(Map.of("message", "Transação deletada com sucesso"));
        } catch (Exception e) {
            log.error("Erro ao deletar transação: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
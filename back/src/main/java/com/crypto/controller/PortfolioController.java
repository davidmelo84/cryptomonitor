
package com.crypto.controller;

import com.crypto.model.Transaction;
import com.crypto.service.PortfolioService;
import com.crypto.util.InputSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    private final InputSanitizer sanitizer;


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


    @PostMapping("/transaction")
    public ResponseEntity<?> addTransaction(
            @RequestBody Transaction transaction,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();

            transaction.setCoinSymbol(
                    sanitizer.sanitizeCoinSymbol(transaction.getCoinSymbol())
            );

            transaction.setCoinName(
                    sanitizer.validateAndSanitize(transaction.getCoinName(), "coinName")
            );

            if (transaction.getQuantity() == null ||
                    transaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Quantidade deve ser maior que zero"));
            }

            if (transaction.getPricePerUnit() == null ||
                    transaction.getPricePerUnit().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Preço deve ser maior que zero"));
            }

            Transaction savedTransaction = portfolioService.addTransaction(username, transaction);
            return ResponseEntity.ok(savedTransaction);

        } catch (IllegalArgumentException e) {
            log.error("❌ Input inválido: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erro ao adicionar transação: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }


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

// back/src/main/java/com/crypto/service/PortfolioService.java

package com.crypto.service;

import com.crypto.model.CryptoCurrency;
import com.crypto.model.Portfolio;
import com.crypto.model.Transaction;
import com.crypto.model.User;
import com.crypto.repository.PortfolioRepository;
import com.crypto.repository.TransactionRepository;
import com.crypto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    /**
     * Adiciona uma transação e atualiza o portfolio
     */
    @Transactional
    public Transaction addTransaction(String username, Transaction transaction) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        transaction.setUser(user);
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Atualizar portfolio
        updatePortfolio(user, transaction);

        log.info("Transação adicionada: {} {} {} @ {}",
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getCoinSymbol(),
                transaction.getPricePerUnit());

        return savedTransaction;
    }

    /**
     * Atualiza o portfolio com base na transação
     */
    private void updatePortfolio(User user, Transaction transaction) {
        Optional<Portfolio> existingPortfolio = portfolioRepository
                .findByUserAndCoinSymbol(user, transaction.getCoinSymbol());

        if (transaction.getType() == Transaction.TransactionType.BUY) {
            handleBuyTransaction(user, transaction, existingPortfolio);
        } else {
            handleSellTransaction(user, transaction, existingPortfolio);
        }
    }

    /**
     * Processa compra
     */
    private void handleBuyTransaction(User user, Transaction transaction, Optional<Portfolio> existingPortfolio) {
        if (existingPortfolio.isPresent()) {
            Portfolio portfolio = existingPortfolio.get();

            // Calcular novo custo médio
            BigDecimal currentTotal = portfolio.getTotalInvested();
            BigDecimal newTotal = currentTotal.add(transaction.getTotalValue());

            BigDecimal currentQuantity = portfolio.getQuantity();
            BigDecimal newQuantity = currentQuantity.add(transaction.getQuantity());

            BigDecimal newAveragePrice = newTotal.divide(newQuantity, 8, RoundingMode.HALF_UP);

            portfolio.setQuantity(newQuantity);
            portfolio.setAverageBuyPrice(newAveragePrice);
            portfolio.setTotalInvested(newTotal);

            portfolioRepository.save(portfolio);
        } else {
            // Criar novo portfolio
            Portfolio newPortfolio = Portfolio.builder()
                    .user(user)
                    .coinSymbol(transaction.getCoinSymbol())
                    .coinName(transaction.getCoinName())
                    .quantity(transaction.getQuantity())
                    .averageBuyPrice(transaction.getPricePerUnit())
                    .totalInvested(transaction.getTotalValue())
                    .build();

            portfolioRepository.save(newPortfolio);
        }
    }

    /**
     * Processa venda
     */
    private void handleSellTransaction(User user, Transaction transaction, Optional<Portfolio> existingPortfolio) {
        if (existingPortfolio.isEmpty()) {
            throw new RuntimeException("Você não possui " + transaction.getCoinSymbol() + " para vender");
        }

        Portfolio portfolio = existingPortfolio.get();

        if (portfolio.getQuantity().compareTo(transaction.getQuantity()) < 0) {
            throw new RuntimeException("Quantidade insuficiente. Você possui apenas " +
                    portfolio.getQuantity() + " " + transaction.getCoinSymbol());
        }

        // Calcular novo total investido (proporcionalmente)
        BigDecimal percentageSold = transaction.getQuantity()
                .divide(portfolio.getQuantity(), 8, RoundingMode.HALF_UP);
        BigDecimal investmentSold = portfolio.getTotalInvested().multiply(percentageSold);

        BigDecimal newQuantity = portfolio.getQuantity().subtract(transaction.getQuantity());
        BigDecimal newTotalInvested = portfolio.getTotalInvested().subtract(investmentSold);

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // Vendeu tudo, remover do portfolio
            portfolioRepository.delete(portfolio);
        } else {
            portfolio.setQuantity(newQuantity);
            portfolio.setTotalInvested(newTotalInvested);
            portfolioRepository.save(portfolio);
        }
    }

    /**
     * ✅ SPRINT 2 - Otimizado com Projeção
     *
     * Usa PortfolioProjection para evitar carregar User completo
     */
    public Map<String, Object> getPortfolio(String username) {
        // ✅ USA PROJEÇÃO ao invés de carregar entidade completa
        List<PortfolioRepository.PortfolioProjection> portfolios =
                portfolioRepository.findByUserUsernameOptimized(username);

        // Buscar preços atuais
        List<CryptoCurrency> currentPrices = cryptoService.getCurrentPrices();
        Map<String, BigDecimal> priceMap = currentPrices.stream()
                .collect(Collectors.toMap(
                        c -> c.getSymbol().toUpperCase(),
                        CryptoCurrency::getCurrentPrice,
                        (a, b) -> a
                ));

        // Calcular valores atuais e lucro/prejuízo
        List<Map<String, Object>> enrichedPortfolio = new ArrayList<>();
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;

        for (PortfolioRepository.PortfolioProjection p : portfolios) {
            BigDecimal currentPrice = priceMap.getOrDefault(
                    p.getCoinSymbol().toUpperCase(),
                    p.getAverageBuyPrice()
            );

            BigDecimal currentValue = p.getQuantity().multiply(currentPrice);
            BigDecimal profitLoss = currentValue.subtract(p.getTotalInvested());
            BigDecimal profitLossPercent = p.getTotalInvested().compareTo(BigDecimal.ZERO) > 0
                    ? profitLoss.divide(p.getTotalInvested(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("coinSymbol", p.getCoinSymbol());
            item.put("coinName", p.getCoinName());
            item.put("quantity", p.getQuantity());
            item.put("averageBuyPrice", p.getAverageBuyPrice());
            item.put("currentPrice", currentPrice);
            item.put("totalInvested", p.getTotalInvested());
            item.put("currentValue", currentValue);
            item.put("profitLoss", profitLoss);
            item.put("profitLossPercent", profitLossPercent);

            enrichedPortfolio.add(item);

            totalInvested = totalInvested.add(p.getTotalInvested());
            totalCurrentValue = totalCurrentValue.add(currentValue);
        }

        BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);
        BigDecimal totalProfitLossPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? totalProfitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("portfolio", enrichedPortfolio);
        result.put("totalInvested", totalInvested);
        result.put("totalCurrentValue", totalCurrentValue);
        result.put("totalProfitLoss", totalProfitLoss);
        result.put("totalProfitLossPercent", totalProfitLossPercent);

        log.debug("✅ Portfolio otimizado carregado para {}: {} itens",
                username, portfolios.size());

        return result;
    }

    /**
     * Busca histórico de transações
     */
    public List<Transaction> getTransactions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return transactionRepository.findByUserOrderByTransactionDateDesc(user);
    }

    /**
     * Deleta uma transação (não recomendado, mas útil para correções)
     */
    @Transactional
    public void deleteTransaction(String username, Long transactionId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para deletar esta transação");
        }

        transactionRepository.delete(transaction);

        // Recalcular portfolio (simplificado - em produção, seria mais complexo)
        log.warn("Transação deletada. Portfolio pode precisar de recálculo manual.");
    }
}
package com.crypto.service;

import com.crypto.exception.PortfolioException;
import com.crypto.exception.UserNotFoundException;
import com.crypto.model.*;
import com.crypto.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ REFATORADO - Exceções específicas e tratamento robusto
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    @Transactional
    public Transaction addTransaction(String username, Transaction transaction) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        validateTransaction(transaction);

        transaction.setUser(user);
        Transaction savedTransaction = transactionRepository.save(transaction);

        updatePortfolio(user, transaction);

        log.info("Transação adicionada: {} {} {} @ {}",
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getCoinSymbol(),
                transaction.getPricePerUnit());

        return savedTransaction;
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction.getQuantity() == null ||
                transaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PortfolioException(
                    "Quantidade deve ser maior que zero",
                    PortfolioException.ErrorType.INVALID_QUANTITY
            );
        }

        if (transaction.getPricePerUnit() == null ||
                transaction.getPricePerUnit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PortfolioException(
                    "Preço deve ser maior que zero",
                    PortfolioException.ErrorType.INVALID_QUANTITY
            );
        }

        if (transaction.getCoinSymbol() == null ||
                transaction.getCoinSymbol().trim().isEmpty()) {
            throw new PortfolioException(
                    "Símbolo da moeda é obrigatório",
                    PortfolioException.ErrorType.CRYPTO_NOT_FOUND
            );
        }
    }

    private void updatePortfolio(User user, Transaction transaction) {
        Optional<Portfolio> existingPortfolio = portfolioRepository
                .findByUserAndCoinSymbol(user, transaction.getCoinSymbol());

        try {
            if (transaction.getType() == Transaction.TransactionType.BUY) {
                handleBuyTransaction(user, transaction, existingPortfolio);
            } else {
                handleSellTransaction(user, transaction, existingPortfolio);
            }
        } catch (ArithmeticException e) {
            throw new PortfolioException(
                    "Erro no cálculo do portfolio: " + e.getMessage(),
                    PortfolioException.ErrorType.TRANSACTION_FAILED,
                    e
            );
        }
    }

    private void handleBuyTransaction(User user, Transaction transaction,
                                      Optional<Portfolio> existingPortfolio) {
        if (existingPortfolio.isPresent()) {
            Portfolio portfolio = existingPortfolio.get();

            BigDecimal currentTotal = portfolio.getTotalInvested();
            BigDecimal newTotal = currentTotal.add(transaction.getTotalValue());

            BigDecimal currentQuantity = portfolio.getQuantity();
            BigDecimal newQuantity = currentQuantity.add(transaction.getQuantity());

            BigDecimal newAveragePrice = newTotal.divide(
                    newQuantity, 8, RoundingMode.HALF_UP);

            portfolio.setQuantity(newQuantity);
            portfolio.setAverageBuyPrice(newAveragePrice);
            portfolio.setTotalInvested(newTotal);

            portfolioRepository.save(portfolio);

            log.debug("Portfolio atualizado: {} - Nova quantidade: {}",
                    transaction.getCoinSymbol(), newQuantity);
        } else {
            Portfolio newPortfolio = Portfolio.builder()
                    .user(user)
                    .coinSymbol(transaction.getCoinSymbol())
                    .coinName(transaction.getCoinName())
                    .quantity(transaction.getQuantity())
                    .averageBuyPrice(transaction.getPricePerUnit())
                    .totalInvested(transaction.getTotalValue())
                    .build();

            portfolioRepository.save(newPortfolio);

            log.debug("Novo portfolio criado: {}", transaction.getCoinSymbol());
        }
    }

    private void handleSellTransaction(User user, Transaction transaction,
                                       Optional<Portfolio> existingPortfolio) {
        if (existingPortfolio.isEmpty()) {
            throw new PortfolioException(
                    String.format("Você não possui %s para vender", transaction.getCoinSymbol()),
                    PortfolioException.ErrorType.CRYPTO_NOT_FOUND
            );
        }

        Portfolio portfolio = existingPortfolio.get();

        if (portfolio.getQuantity().compareTo(transaction.getQuantity()) < 0) {
            throw new PortfolioException(
                    String.format("Quantidade insuficiente. Você possui apenas %s %s",
                            portfolio.getQuantity(), transaction.getCoinSymbol()),
                    PortfolioException.ErrorType.INSUFFICIENT_BALANCE
            );
        }

        BigDecimal percentageSold = transaction.getQuantity()
                .divide(portfolio.getQuantity(), 8, RoundingMode.HALF_UP);
        BigDecimal investmentSold = portfolio.getTotalInvested().multiply(percentageSold);

        BigDecimal newQuantity = portfolio.getQuantity().subtract(transaction.getQuantity());
        BigDecimal newTotalInvested = portfolio.getTotalInvested().subtract(investmentSold);

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            portfolioRepository.delete(portfolio);
            log.debug("Portfolio removido: {} (venda total)", transaction.getCoinSymbol());
        } else {
            portfolio.setQuantity(newQuantity);
            portfolio.setTotalInvested(newTotalInvested);
            portfolioRepository.save(portfolio);
            log.debug("Portfolio atualizado após venda: {} - Restante: {}",
                    transaction.getCoinSymbol(), newQuantity);
        }
    }

    public Map<String, Object> getPortfolio(String username) {
        try {
            List<PortfolioRepository.PortfolioProjection> portfolios =
                    portfolioRepository.findByUserUsernameOptimized(username);

            List<String> coinIds = portfolios.stream()
                    .map(p -> mapSymbolToCoinId(p.getCoinSymbol()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<CryptoCurrency> currentPrices = cryptoService.getPricesByIds(coinIds);

            Map<String, BigDecimal> priceMap = currentPrices.stream()
                    .collect(Collectors.toMap(
                            c -> c.getSymbol().toUpperCase(),
                            CryptoCurrency::getCurrentPrice,
                            (a, b) -> a
                    ));

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

            log.debug("Portfolio carregado para {}: {} itens", username, portfolios.size());

            return result;

        } catch (Exception e) {
            log.error("Erro ao carregar portfolio de {}: {}", username, e.getMessage(), e);
            throw new PortfolioException(
                    "Erro ao carregar portfolio",
                    PortfolioException.ErrorType.TRANSACTION_FAILED,
                    e
            );
        }
    }

    public List<Transaction> getTransactions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        return transactionRepository.findByUserOrderByTransactionDateDesc(user);
    }

    @Transactional
    public void deleteTransaction(String username, Long transactionId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new PortfolioException(
                        "Transação não encontrada",
                        PortfolioException.ErrorType.TRANSACTION_FAILED
                ));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new PortfolioException(
                    "Você não tem permissão para deletar esta transação",
                    PortfolioException.ErrorType.TRANSACTION_FAILED
            );
        }

        transactionRepository.delete(transaction);

        log.warn("Transação deletada por {}: ID {}", username, transactionId);
    }

    private String mapSymbolToCoinId(String symbol) {
        Map<String, String> symbolMap = Map.ofEntries(
                Map.entry("BTC", "bitcoin"),
                Map.entry("ETH", "ethereum"),
                Map.entry("ADA", "cardano"),
                Map.entry("DOT", "polkadot"),
                Map.entry("LINK", "chainlink"),
                Map.entry("SOL", "solana"),
                Map.entry("AVAX", "avalanche-2"),
                Map.entry("MATIC", "matic-network"),
                Map.entry("LTC", "litecoin"),
                Map.entry("BCH", "bitcoin-cash"),
                Map.entry("XRP", "ripple"),
                Map.entry("DOGE", "dogecoin"),
                Map.entry("BNB", "binancecoin")
        );

        String upperSymbol = symbol.toUpperCase();
        return symbolMap.getOrDefault(upperSymbol, symbol.toLowerCase());
    }
}
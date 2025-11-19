package com.crypto.service;

import com.crypto.model.*;
import com.crypto.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ✅ TESTES UNITÁRIOS - PortfolioService
 *
 * Testa lógica de compra/venda e cálculo de portfolio
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService - Testes Unitários")
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private PortfolioService portfolioService;

    private User testUser;
    private CryptoCurrency testCrypto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testCrypto = CryptoCurrency.builder()
                .coinId("bitcoin")
                .symbol("BTC")
                .name("Bitcoin")
                .currentPrice(new BigDecimal("50000.00"))
                .build();
    }

    @Test
    @DisplayName("Deve adicionar transação de compra")
    void shouldAddBuyTransaction() {
        // Arrange
        Transaction buyTransaction = Transaction.builder()
                .coinSymbol("BTC")
                .coinName("Bitcoin")
                .type(Transaction.TransactionType.BUY)
                .quantity(new BigDecimal("0.1"))
                .pricePerUnit(new BigDecimal("50000.00"))
                .build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(buyTransaction);

        when(portfolioRepository.findByUserAndCoinSymbol(testUser, "BTC"))
                .thenReturn(Optional.empty());

        // Act
        Transaction result = portfolioService.addTransaction("testuser", buyTransaction);

        // Assert
        assertThat(result).isNotNull();
        verify(transactionRepository).save(any(Transaction.class));
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("Deve atualizar portfolio existente em compra")
    void shouldUpdateExistingPortfolioOnBuy() {
        // Arrange
        Portfolio existingPortfolio = Portfolio.builder()
                .user(testUser)
                .coinSymbol("BTC")
                .quantity(new BigDecimal("0.5"))
                .averageBuyPrice(new BigDecimal("45000.00"))
                .totalInvested(new BigDecimal("22500.00"))
                .build();

        Transaction buyTransaction = Transaction.builder()
                .coinSymbol("BTC")
                .type(Transaction.TransactionType.BUY)
                .quantity(new BigDecimal("0.1"))
                .pricePerUnit(new BigDecimal("50000.00"))
                .totalValue(new BigDecimal("5000.00"))
                .build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        when(portfolioRepository.findByUserAndCoinSymbol(testUser, "BTC"))
                .thenReturn(Optional.of(existingPortfolio));

        when(transactionRepository.save(any()))
                .thenReturn(buyTransaction);

        // Act
        portfolioService.addTransaction("testuser", buyTransaction);

        // Assert
        verify(portfolioRepository).save(argThat(portfolio -> {
            // Novo custo médio deve ser calculado corretamente
            BigDecimal expectedQuantity = new BigDecimal("0.6"); // 0.5 + 0.1
            BigDecimal expectedTotal = new BigDecimal("27500.00"); // 22500 + 5000

            return portfolio.getQuantity().compareTo(expectedQuantity) == 0 &&
                    portfolio.getTotalInvested().compareTo(expectedTotal) == 0;
        }));
    }

    @Test
    @DisplayName("Deve lançar exceção ao vender sem saldo")
    void shouldThrowExceptionWhenSellingWithoutBalance() {
        // Arrange
        Transaction sellTransaction = Transaction.builder()
                .coinSymbol("BTC")
                .type(Transaction.TransactionType.SELL)
                .quantity(new BigDecimal("1.0"))
                .pricePerUnit(new BigDecimal("50000.00"))
                .build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        when(portfolioRepository.findByUserAndCoinSymbol(testUser, "BTC"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                portfolioService.addTransaction("testuser", sellTransaction)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não possui");
    }

    @Test
    @DisplayName("Deve remover portfolio ao vender tudo")
    void shouldRemovePortfolioWhenSellingAll() {
        // Arrange
        Portfolio portfolio = Portfolio.builder()
                .user(testUser)
                .coinSymbol("BTC")
                .quantity(new BigDecimal("0.5"))
                .averageBuyPrice(new BigDecimal("45000.00"))
                .totalInvested(new BigDecimal("22500.00"))
                .build();

        Transaction sellTransaction = Transaction.builder()
                .coinSymbol("BTC")
                .type(Transaction.TransactionType.SELL)
                .quantity(new BigDecimal("0.5"))
                .pricePerUnit(new BigDecimal("50000.00"))
                .build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        when(portfolioRepository.findByUserAndCoinSymbol(testUser, "BTC"))
                .thenReturn(Optional.of(portfolio));

        when(transactionRepository.save(any()))
                .thenReturn(sellTransaction);

        // Act
        portfolioService.addTransaction("testuser", sellTransaction);

        // Assert
        verify(portfolioRepository).delete(portfolio);
        verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve calcular portfolio com lucro/prejuízo")
    void shouldCalculatePortfolioWithProfitLoss() {
        // Arrange
        PortfolioRepository.PortfolioProjection projection =
                mock(PortfolioRepository.PortfolioProjection.class);

        when(projection.getId()).thenReturn(1L);
        when(projection.getCoinSymbol()).thenReturn("BTC");
        when(projection.getCoinName()).thenReturn("Bitcoin");
        when(projection.getQuantity()).thenReturn(new BigDecimal("0.5"));
        when(projection.getAverageBuyPrice()).thenReturn(new BigDecimal("45000.00"));
        when(projection.getTotalInvested()).thenReturn(new BigDecimal("22500.00"));

        when(portfolioRepository.findByUserUsernameOptimized("testuser"))
                .thenReturn(List.of(projection));

        when(cryptoService.getCurrentPrices())
                .thenReturn(List.of(testCrypto));

        // Act
        Map<String, Object> result = portfolioService.getPortfolio("testuser");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).containsKeys(
                "portfolio",
                "totalInvested",
                "totalCurrentValue",
                "totalProfitLoss"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> portfolio =
                (List<Map<String, Object>>) result.get("portfolio");

        assertThat(portfolio).hasSize(1);

        Map<String, Object> item = portfolio.get(0);
        assertThat(item.get("coinSymbol")).isEqualTo("BTC");

        // Preço atual: 50000, comprou a 45000
        // Lucro: (50000 - 45000) * 0.5 = 2500
        BigDecimal profitLoss = (BigDecimal) item.get("profitLoss");
        assertThat(profitLoss).isEqualByComparingTo(new BigDecimal("2500.00"));
    }

    @Test
    @DisplayName("Deve listar transações do usuário")
    void shouldListUserTransactions() {
        // Arrange
        List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                        .coinSymbol("BTC")
                        .type(Transaction.TransactionType.BUY)
                        .quantity(new BigDecimal("0.5"))
                        .build(),
                Transaction.builder()
                        .coinSymbol("ETH")
                        .type(Transaction.TransactionType.BUY)
                        .quantity(new BigDecimal("2.0"))
                        .build()
        );

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        when(transactionRepository.findByUserOrderByTransactionDateDesc(testUser))
                .thenReturn(transactions);

        // Act
        List<Transaction> result = portfolioService.getTransactions("testuser");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCoinSymbol()).isEqualTo("BTC");
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar transação de outro usuário")
    void shouldThrowExceptionWhenDeletingOtherUserTransaction() {
        // Arrange
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .build();

        Transaction transaction = Transaction.builder()
                .id(1L)
                .user(otherUser)
                .build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        // Act & Assert
        assertThatThrownBy(() ->
                portfolioService.deleteTransaction("testuser", 1L)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("permissão");
    }
}
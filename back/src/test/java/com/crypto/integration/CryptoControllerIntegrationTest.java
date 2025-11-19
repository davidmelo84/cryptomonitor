package com.crypto.integration;

import com.crypto.model.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ✅ TESTE DE INTEGRAÇÃO - CryptoController
 *
 * Testa endpoints públicos de consulta de preços
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CryptoController - Testes de Integração")
class CryptoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CryptoCurrencyRepository cryptoRepository;

    private static final String BASE_URL = "/crypto-monitor/api/crypto";

    @BeforeEach
    void setUp() {
        cryptoRepository.deleteAll();
        seedDatabase();
    }

    private void seedDatabase() {
        CryptoCurrency btc = CryptoCurrency.builder()
                .coinId("bitcoin")
                .symbol("BTC")
                .name("Bitcoin")
                .currentPrice(new BigDecimal("50000.00"))
                .priceChange24h(5.5)
                .priceChange7d(10.2)
                .marketCap(new BigDecimal("1000000000000"))
                .totalVolume(new BigDecimal("50000000000"))
                .lastUpdated(LocalDateTime.now())
                .build();

        CryptoCurrency eth = CryptoCurrency.builder()
                .coinId("ethereum")
                .symbol("ETH")
                .name("Ethereum")
                .currentPrice(new BigDecimal("3000.00"))
                .priceChange24h(-2.3)
                .priceChange7d(5.1)
                .marketCap(new BigDecimal("500000000000"))
                .totalVolume(new BigDecimal("20000000000"))
                .lastUpdated(LocalDateTime.now())
                .build();

        cryptoRepository.save(btc);
        cryptoRepository.save(eth);
    }

    @Test
    @DisplayName("Deve retornar lista de criptomoedas")
    void shouldReturnCryptoList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").exists())
                .andExpect(jsonPath("$[0].currentPrice").exists());
    }

    @Test
    @DisplayName("Deve retornar criptomoeda específica por coinId")
    void shouldReturnSpecificCrypto() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current/bitcoin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coinId").value("bitcoin"))
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.name").value("Bitcoin"))
                .andExpect(jsonPath("$.currentPrice").value(50000.00));
    }

    @Test
    @DisplayName("Deve retornar 404 para coinId inexistente")
    void shouldReturn404ForInvalidCoinId() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current/invalid-coin"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar status da API")
    void shouldReturnApiStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("CoinGecko"))
                .andExpect(jsonPath("$.smartCache").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Deve validar sanitização de coinId com SQL injection")
    void shouldRejectSqlInjectionInCoinId() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current/bitcoin' OR '1'='1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve validar sanitização de coinId com XSS")
    void shouldRejectXssInCoinId() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current/<script>alert('xss')</script>"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve validar parâmetro days no histórico")
    void shouldValidateDaysParameter() throws Exception {
        // Days muito grande (> 365)
        mockMvc.perform(get(BASE_URL + "/history/bitcoin?days=400"))
                .andExpect(status().isBadRequest());

        // Days negativo
        mockMvc.perform(get(BASE_URL + "/history/bitcoin?days=-5"))
                .andExpect(status().isBadRequest());

        // Days válido
        mockMvc.perform(get(BASE_URL + "/history/bitcoin?days=7"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve respeitar cache headers")
    void shouldRespectCacheHeaders() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("Cache-Control", "max-age=300"));
    }

    @Test
    @DisplayName("Deve retornar JSON válido para todas moedas")
    void shouldReturnValidJsonForAllCoins() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$[*].coinId").exists())
                .andExpect(jsonPath("$[*].symbol").exists())
                .andExpect(jsonPath("$[*].currentPrice").exists())
                .andExpect(jsonPath("$[*].priceChange24h").exists());
    }
}
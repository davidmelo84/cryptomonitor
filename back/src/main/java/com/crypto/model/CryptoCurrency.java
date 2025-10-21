package com.crypto.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cryptocurrencies")
public class CryptoCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore // Não mapeia esse campo no JSON
    private Long id;

    @Column(unique = true, nullable = false)
    @JsonProperty("id") // Esse será mapeado do JSON
    private String coinId;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("name")
    private String name;

    @JsonProperty("current_price")
    private BigDecimal currentPrice;

    @JsonProperty("price_change_percentage_1h_in_currency")
    private Double priceChange1h;

    @JsonProperty("price_change_percentage_24h")
    private Double priceChange24h;

    @JsonProperty("price_change_percentage_7d_in_currency")
    private Double priceChange7d;

    @JsonProperty("market_cap")
    private BigDecimal marketCap;

    @JsonProperty("total_volume")
    private BigDecimal totalVolume;

    private LocalDateTime lastUpdated = LocalDateTime.now();
}

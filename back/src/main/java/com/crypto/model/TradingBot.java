package com.crypto.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trading_bots")
public class TradingBot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "coin_symbol", nullable = false)
    private String coinSymbol;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false)
    private TradingStrategy strategy = TradingStrategy.GRID_TRADING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BotStatus status = BotStatus.STOPPED;

    @Builder.Default
    @Column(name = "is_simulation", nullable = false)
    private Boolean isSimulation = true;

    @Column(name = "grid_lower_price", precision = 19, scale = 8)
    private BigDecimal gridLowerPrice;

    @Column(name = "grid_upper_price", precision = 19, scale = 8)
    private BigDecimal gridUpperPrice;

    @Column(name = "grid_levels")
    private Integer gridLevels;

    @Column(name = "amount_per_grid", precision = 19, scale = 8)
    private BigDecimal amountPerGrid;

    @Column(name = "dca_amount", precision = 19, scale = 2)
    private BigDecimal dcaAmount;

    @Column(name = "dca_interval_minutes")
    private Integer dcaIntervalMinutes;

    @Column(name = "last_dca_execution")
    private LocalDateTime lastDcaExecution;

    @Column(name = "stop_loss_percent", precision = 5, scale = 2)
    private BigDecimal stopLossPercent;

    @Column(name = "take_profit_percent", precision = 5, scale = 2)
    private BigDecimal takeProfitPercent;

    @Column(name = "entry_price", precision = 19, scale = 8)
    private BigDecimal entryPrice;

    @Builder.Default
    @Column(name = "total_profit_loss", precision = 19, scale = 2)
    private BigDecimal totalProfitLoss = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_trades")
    private Integer totalTrades = 0;

    @Builder.Default
    @Column(name = "winning_trades")
    private Integer winningTrades = 0;

    @Builder.Default
    @Column(name = "losing_trades")
    private Integer losingTrades = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        validateSimulation();
    }


    @PreUpdate
    protected void onUpdate() {
        validateSimulation();
    }


    private void validateSimulation() {
        if (isSimulation == null) {
            isSimulation = true;
        }

        if (!isSimulation) {
            throw new UnsupportedOperationException(
                    "🚨 TRADING REAL DESABILITADO POR SEGURANÇA!\n\n" +
                            "Este sistema NÃO está conectado a exchanges reais.\n" +
                            "Apenas simulações são permitidas.\n\n" +
                            "Para trading real, use plataformas oficiais:\n" +
                            "- Binance API\n" +
                            "- Coinbase Pro\n" +
                            "- FTX\n" +
                            "- Kraken"
            );
        }
    }



    public enum TradingStrategy {
        GRID_TRADING,
        DCA,
        STOP_LOSS,
        CUSTOM
    }

    public enum BotStatus {
        RUNNING,
        STOPPED,
        PAUSED,
        ERROR
    }
}
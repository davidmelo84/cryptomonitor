package com.crypto.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "coin_symbol", length = 10, nullable = false)
    private String coinSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(name = "threshold_value", precision = 10, scale = 6)
    private BigDecimal thresholdValue;

    @Column(name = "target_price", precision = 20, scale = 8, nullable = true)
    private BigDecimal targetPrice;

    @Column(name = "time_period", length = 20)
    private String timePeriod;

    @Email(message = "E-mail inválido")
    @NotBlank(message = "E-mail é obrigatório")
    @Column(name = "notification_email", nullable = false)
    private String notificationEmail;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ✅ ENUMS DENTRO DA CLASSE (NÃO ESQUEÇA DELES!)
    public enum AlertType {
        PRICE_INCREASE,
        PRICE_DECREASE,
        VOLUME_SPIKE,
        PERCENT_CHANGE_24H,
        MARKET_CAP
    }

    public enum TimePeriod {
        ONE_HOUR,
        TWENTY_FOUR_HOURS,
        SEVEN_DAYS
    }
} // ✅ ESTA CHAVE FECHA A CLASSE
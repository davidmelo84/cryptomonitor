// back/src/main/java/com/crypto/model/AlertRule.java
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

    // ✅ Usado para PERCENT_CHANGE_24H (percentual)
    @Column(name = "threshold_value", precision = 10, scale = 6)
    private BigDecimal thresholdValue;

    // ✅ Usado para PRICE_INCREASE/PRICE_DECREASE (preço absoluto)
    // ✅ CORRIGIDO: Agora pode ser NULL
    @Column(name = "target_price", precision = 20, scale = 8, nullable = true)
    private BigDecimal targetPrice;

    @Column(name = "time_period", length = 20)
    private String timePeriod;

    @Email(message = "E-mail inválido")
    @NotBlank(message = "E-mail é obrigatório")
    @Column(name = "notification_email", nullable = false)
    private String notificationEmail;

    // ✅ CORRIGIDO: Campo correto é "active", não "isActive"
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ✅ ENUMS DENTRO DA CLASSE
    public enum AlertType {
        PRICE_INCREASE,      // Preço atingiu valor (usa targetPrice)
        PRICE_DECREASE,      // Preço caiu abaixo de valor (usa targetPrice)
        VOLUME_SPIKE,        // Volume disparou (usa thresholdValue)
        PERCENT_CHANGE_24H,  // Variação percentual 24h (usa thresholdValue)
        MARKET_CAP           // Market cap (usa thresholdValue)
    }

    public enum TimePeriod {
        ONE_HOUR,
        TWENTY_FOUR_HOURS,
        SEVEN_DAYS
    }

    // ✅ Getter/Setter customizados para compatibilidade
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // ✅ Alias para código legado
    public Boolean getIsActive() {
        return active;
    }

    public void setIsActive(Boolean active) {
        this.active = active;
    }
}
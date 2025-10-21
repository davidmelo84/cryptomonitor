package com.crypto.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin_symbol")
    private String coinSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type")
    private AlertType alertType;

    @Column(name = "threshold_value", precision = 19, scale = 6)
    private BigDecimal thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_period")
    private TimePeriod timePeriod;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "notification_email")
    private String notificationEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Implementação correta dos setters customizados
    public void setTargetValue(
            @NotNull(message = "Valor alvo é obrigatório")
            @DecimalMin(value = "0.0", inclusive = false, message = "Valor deve ser maior que zero")
            BigDecimal targetValue) {
        this.thresholdValue = targetValue;
    }

    public void setEmail(
            @Email(message = "E-mail inválido")
            @NotBlank(message = "E-mail é obrigatório")
            String email) {
        this.notificationEmail = email;
    }

    public enum AlertType {
        PRICE_INCREASE,      // preço acima do limite
        PRICE_DECREASE,      // preço abaixo do limite
        VOLUME_SPIKE,        // volume disparou
        PERCENT_CHANGE_24H,  // variação percentual em 24h
        MARKET_CAP           // valor de mercado
    }

    public enum TimePeriod {
        ONE_HOUR, TWENTY_FOUR_HOURS, SEVEN_DAYS
    }
}

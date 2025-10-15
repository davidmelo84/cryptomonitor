package com.crypto.model.dto;

import com.crypto.model.AlertRule;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlertRuleDTO {

    @NotBlank(message = "Símbolo da moeda é obrigatório")
    @Size(min = 2, max = 10, message = "Símbolo deve ter entre 2 e 10 caracteres")
    private String coinSymbol;

    @NotNull(message = "Tipo de alerta é obrigatório")
    private AlertRule.AlertType alertType;

    @NotNull(message = "Valor alvo é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Valor deve ser maior que zero")
    private BigDecimal targetValue;

    @Email(message = "E-mail inválido")
    @NotBlank(message = "E-mail é obrigatório")
    private String email;

    public AlertRule toEntity() {
        AlertRule alertRule = new AlertRule();
        alertRule.setCoinSymbol(this.coinSymbol);
        alertRule.setAlertType(this.alertType);
        alertRule.setTargetValue(this.targetValue);
        alertRule.setEmail(this.email);
        alertRule.setActive(true); // nova regra sempre ativa por padrão
        return alertRule;
    }
}

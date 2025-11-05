// ========================================
// 3. AlertRuleDTO.java (CORRIGIDO)
// ========================================
package com.crypto.model.dto;

import com.crypto.model.AlertRule;
import com.crypto.model.AlertRule.AlertType;  // ✅ ADICIONE ESTA LINHA
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleDTO {

    @NotBlank(message = "Símbolo da moeda é obrigatório")
    @Size(min = 2, max = 10, message = "Símbolo deve ter entre 2 e 10 caracteres")
    private String coinSymbol;

    @NotNull(message = "Tipo de alerta é obrigatório")
    private AlertType alertType;

    @NotNull(message = "Valor threshold é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Valor deve ser maior que zero")
    private BigDecimal thresholdValue; // ✅ Renomeado de targetValue para thresholdValue

    @Email(message = "E-mail inválido")
    @NotBlank(message = "E-mail é obrigatório")
    private String email;

    // ✅ Converte DTO → Entity
    public AlertRule toEntity() {
        AlertRule alertRule = new AlertRule();
        alertRule.setCoinSymbol(this.coinSymbol);
        alertRule.setAlertType(this.alertType);
        alertRule.setThresholdValue(this.thresholdValue);  // ✅ Correto
        alertRule.setNotificationEmail(this.email);        // ✅ Correto
        alertRule.setTargetPrice(null);                    // ✅ Sempre null (campo legado)
        alertRule.setActive(true);                         // ✅ Nova regra sempre ativa
        return alertRule;
    }

    // ✅ Converte Entity → DTO
    public static AlertRuleDTO fromEntity(AlertRule alertRule) {
        return AlertRuleDTO.builder()
                .coinSymbol(alertRule.getCoinSymbol())
                .alertType(alertRule.getAlertType())
                .thresholdValue(alertRule.getThresholdValue())
                .email(alertRule.getNotificationEmail())
                .build();
    }
}

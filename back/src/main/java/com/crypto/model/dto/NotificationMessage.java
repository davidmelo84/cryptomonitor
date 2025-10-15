package com.crypto.model.dto;

import com.crypto.model.AlertRule.AlertType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationMessage {
    private String coinSymbol;
    private String coinName;
    private String currentPrice;
    private String changePercentage;
    private AlertType alertType;
    private String message;
    private String recipient;
}

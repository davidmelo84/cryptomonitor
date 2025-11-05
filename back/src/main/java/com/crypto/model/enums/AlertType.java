package com.crypto.model.enums;

public enum AlertType {
    PRICE_INCREASE,      // preço acima do limite
    PRICE_DECREASE,      // preço abaixo do limite
    VOLUME_SPIKE,        // volume disparou
    PERCENT_CHANGE_24H,  // variação percentual em 24h
    MARKET_CAP           // valor de mercado
}
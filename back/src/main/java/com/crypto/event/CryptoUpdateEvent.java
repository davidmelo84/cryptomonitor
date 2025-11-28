package com.crypto.event;

import com.crypto.model.CryptoCurrency;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;


@Getter
public class CryptoUpdateEvent extends ApplicationEvent {

    private final List<CryptoCurrency> cryptoCurrencies;
    private final String userEmail; // null = todos os usuários
    private final UpdateType type;

    public enum UpdateType {
        SCHEDULED_UPDATE,
        MANUAL_UPDATE,
        SINGLE_CRYPTO
    }


    public CryptoUpdateEvent(Object source, List<CryptoCurrency> cryptoCurrencies, UpdateType type) {
        super(source);
        this.cryptoCurrencies = cryptoCurrencies;
        this.userEmail = null;
        this.type = type;
    }


    public CryptoUpdateEvent(Object source, List<CryptoCurrency> cryptoCurrencies, String userEmail, UpdateType type) {
        super(source);
        this.cryptoCurrencies = cryptoCurrencies;
        this.userEmail = userEmail;
        this.type = type;
    }


    public boolean isGlobalUpdate() {
        return userEmail == null;
    }
}
// back/src/main/java/com/crypto/event/CryptoUpdateEvent.java
package com.crypto.event;

import com.crypto.model.CryptoCurrency;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Evento publicado quando criptomoedas são atualizadas
 * Usado para desacoplar serviços e evitar dependências circulares
 */
@Getter
public class CryptoUpdateEvent extends ApplicationEvent {

    private final List<CryptoCurrency> cryptoCurrencies;
    private final String userEmail; // null = todos os usuários
    private final UpdateType type;

    public enum UpdateType {
        SCHEDULED_UPDATE,    // Atualização automática agendada
        MANUAL_UPDATE,       // Atualização manual do usuário
        SINGLE_CRYPTO       // Atualização de uma crypto específica
    }

    /**
     * Construtor para atualizações globais (todos usuários)
     */
    public CryptoUpdateEvent(Object source, List<CryptoCurrency> cryptoCurrencies, UpdateType type) {
        super(source);
        this.cryptoCurrencies = cryptoCurrencies;
        this.userEmail = null;
        this.type = type;
    }

    /**
     * Construtor para atualizações de usuário específico
     */
    public CryptoUpdateEvent(Object source, List<CryptoCurrency> cryptoCurrencies, String userEmail, UpdateType type) {
        super(source);
        this.cryptoCurrencies = cryptoCurrencies;
        this.userEmail = userEmail;
        this.type = type;
    }

    /**
     * Verifica se é uma atualização global (todos usuários)
     */
    public boolean isGlobalUpdate() {
        return userEmail == null;
    }
}
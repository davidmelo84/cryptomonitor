package com.crypto.repository;

import com.crypto.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    // Métodos existentes
    List<AlertRule> findByCoinSymbolAndActiveTrue(String coinSymbol);

    List<AlertRule> findByActiveTrue();

    List<AlertRule> findByCoinSymbolAndNotificationEmailAndActiveTrue(
            String coinSymbol,
            String notificationEmail
    );

    List<AlertRule> findByNotificationEmailAndActiveTrue(String notificationEmail);

    /**
     * 🔥 NOVO: Query otimizada com IN clause
     * Busca apenas alertas para os símbolos relevantes
     */
    @Query("SELECT ar FROM AlertRule ar " +
            "WHERE ar.notificationEmail = :email " +
            "AND ar.coinSymbol IN :symbols " +
            "AND ar.active = true")
    List<AlertRule> findByNotificationEmailAndCoinSymbolInAndActiveTrue(
            @Param("email") String email,
            @Param("symbols") Set<String> symbols
    );
}

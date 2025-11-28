package com.crypto.repository;

import com.crypto.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByCoinSymbolAndActiveTrue(String coinSymbol);
    List<AlertRule> findByActiveTrue();

    List<AlertRule> findByCoinSymbolAndNotificationEmailAndActiveTrue(
            String coinSymbol,
            String notificationEmail
    );

    List<AlertRule> findByNotificationEmailAndActiveTrue(String notificationEmail);

}

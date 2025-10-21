// back/src/main/java/com/crypto/repository/TradingBotRepository.java

package com.crypto.repository;

import com.crypto.model.TradingBot;
import com.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradingBotRepository extends JpaRepository<TradingBot, Long> {
    List<TradingBot> findByUser(User user);
    List<TradingBot> findByUserAndStatus(User user, TradingBot.BotStatus status);
    List<TradingBot> findByStatus(TradingBot.BotStatus status);
}
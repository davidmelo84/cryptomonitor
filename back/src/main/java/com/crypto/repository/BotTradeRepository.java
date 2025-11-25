// back/src/main/java/com/crypto/repository/BotTradeRepository.java

package com.crypto.repository;

import com.crypto.model.BotTrade;
import com.crypto.model.TradingBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotTradeRepository extends JpaRepository<BotTrade, Long> {

    List<BotTrade> findByBotOrderByExecutedAtDesc(TradingBot bot);

    // ============================================
    // ✅ NOVO MÉTODO — FIFO (ordenação crescente)
    // ============================================
    List<BotTrade> findByBotAndSideOrderByExecutedAtAsc(
            TradingBot bot,
            BotTrade.TradeSide side
    );

    List<BotTrade> findByBotAndIsSimulation(TradingBot bot, Boolean isSimulation);
}

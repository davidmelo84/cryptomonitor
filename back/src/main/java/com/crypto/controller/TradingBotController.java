
package com.crypto.controller;

import com.crypto.model.BotTrade;
import com.crypto.model.TradingBot;
import com.crypto.service.TradingBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
public class TradingBotController {

    private final TradingBotService tradingBotService;


    @PostMapping
    public ResponseEntity<?> createBot(@RequestBody TradingBot bot, Authentication auth) {
        try {
            String username = auth.getName();
            TradingBot createdBot = tradingBotService.createBot(username, bot);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bot criado com sucesso!",
                    "bot", createdBot
            ));
        } catch (Exception e) {
            log.error("Erro ao criar bot: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @GetMapping
    public ResponseEntity<?> getUserBots(Authentication auth) {
        try {
            String username = auth.getName();
            List<TradingBot> bots = tradingBotService.getUserBots(username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "bots", bots
            ));
        } catch (Exception e) {
            log.error("Erro ao buscar bots: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{botId}")
    public ResponseEntity<?> getBot(@PathVariable Long botId, Authentication auth) {
        try {
            String username = auth.getName();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bot encontrado"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @PostMapping("/{botId}/start")
    public ResponseEntity<?> startBot(@PathVariable Long botId, Authentication auth) {
        try {
            String username = auth.getName();
            tradingBotService.startBot(username, botId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bot iniciado com sucesso!"
            ));
        } catch (Exception e) {
            log.error("Erro ao iniciar bot: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{botId}/stop")
    public ResponseEntity<?> stopBot(@PathVariable Long botId, Authentication auth) {
        try {
            String username = auth.getName();
            tradingBotService.stopBot(username, botId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bot parado com sucesso!"
            ));
        } catch (Exception e) {
            log.error("Erro ao parar bot: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @GetMapping("/{botId}/trades")
    public ResponseEntity<?> getBotTrades(@PathVariable Long botId, Authentication auth) {
        try {
            String username = auth.getName();
            List<BotTrade> trades = tradingBotService.getBotTrades(username, botId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "trades", trades
            ));
        } catch (Exception e) {
            log.error("Erro ao buscar trades: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @DeleteMapping("/{botId}")
    public ResponseEntity<?> deleteBot(@PathVariable Long botId, Authentication auth) {
        try {
            String username = auth.getName();
            tradingBotService.deleteBot(username, botId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bot deletado com sucesso!"
            ));
        } catch (Exception e) {
            log.error("Erro ao deletar bot: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @GetMapping("/{botId}/stats")
    public ResponseEntity<?> getBotStats(@PathVariable Long botId, Authentication auth) {
        try {
            String username = auth.getName();
            List<BotTrade> trades = tradingBotService.getBotTrades(username, botId);

            int totalTrades = trades.size();
            long buyTrades = trades.stream().filter(t -> t.getSide() == BotTrade.TradeSide.BUY).count();
            long sellTrades = trades.stream().filter(t -> t.getSide() == BotTrade.TradeSide.SELL).count();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", Map.of(
                            "totalTrades", totalTrades,
                            "buyTrades", buyTrades,
                            "sellTrades", sellTrades
                    )
            ));
        } catch (Exception e) {
            log.error("Erro ao buscar estatísticas: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
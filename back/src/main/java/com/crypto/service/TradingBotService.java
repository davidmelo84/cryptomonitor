// back/src/main/java/com/crypto/service/TradingBotService.java

package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.model.*;
import com.crypto.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingBotService {

    private final TradingBotRepository botRepository;
    private final BotTradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;
    private final NotificationService notificationService;

    /**
     * Mapeia símbolo da moeda para o ID usado pela CoinGecko API
     */
    private String mapSymbolToCoinId(String symbol) {
        Map<String, String> symbolMap = Map.ofEntries(
                Map.entry("BTC", "bitcoin"),
                Map.entry("ETH", "ethereum"),
                Map.entry("ADA", "cardano"),
                Map.entry("DOT", "polkadot"),
                Map.entry("LINK", "chainlink"),
                Map.entry("SOL", "solana"),
                Map.entry("AVAX", "avalanche-2"),
                Map.entry("MATIC", "matic-network"),
                Map.entry("LTC", "litecoin"),
                Map.entry("BCH", "bitcoin-cash"),
                Map.entry("XRP", "ripple"),
                Map.entry("DOGE", "dogecoin"),
                Map.entry("BNB", "binancecoin")
        );

        String upperSymbol = symbol.toUpperCase();
        String coinId = symbolMap.getOrDefault(upperSymbol, symbol.toLowerCase());

        log.debug("🔄 Mapeando símbolo {} → coinId {}", symbol, coinId);

        return coinId;
    }

    /**
     * Cria um novo bot de trading
     */
    @Transactional
    public TradingBot createBot(String username, TradingBot bot) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        bot.setUser(user);
        bot.setStatus(TradingBot.BotStatus.STOPPED);
        bot.setCreatedAt(LocalDateTime.now());

        TradingBot savedBot = botRepository.save(bot);
        log.info("Bot criado: {} - {} - {}", savedBot.getName(), savedBot.getStrategy(), savedBot.getCoinSymbol());

        return savedBot;
    }

    /**
     * Inicia um bot
     */
    @Transactional
    public void startBot(String username, Long botId) {
        TradingBot bot = getBotByIdAndUser(botId, username);

        if (bot.getStatus() == TradingBot.BotStatus.RUNNING) {
            throw new RuntimeException("Bot já está rodando");
        }

        bot.setStatus(TradingBot.BotStatus.RUNNING);
        bot.setStartedAt(LocalDateTime.now());

        // Buscar preço atual como entry price
        String coinId = mapSymbolToCoinId(bot.getCoinSymbol());
        Optional<CryptoCurrency> crypto = cryptoService.getCryptoByCoinId(coinId);
        crypto.ifPresent(c -> bot.setEntryPrice(c.getCurrentPrice()));

        botRepository.save(bot);
        log.info("Bot {} iniciado", bot.getName());
    }

    /**
     * Para um bot
     */
    @Transactional
    public void stopBot(String username, Long botId) {
        TradingBot bot = getBotByIdAndUser(botId, username);
        bot.setStatus(TradingBot.BotStatus.STOPPED);
        bot.setStoppedAt(LocalDateTime.now());
        botRepository.save(bot);
        log.info("Bot {} parado", bot.getName());
    }

    /**
     * Scheduler que executa bots ativos
     */
    @Scheduled(fixedDelay = 60000) // A cada 1 minuto
    public void executeBots() {
        log.info("⏰ Scheduler executando... Procurando bots ativos");
        List<TradingBot> runningBots = botRepository.findByStatus(TradingBot.BotStatus.RUNNING);
        log.info("📊 Encontrados {} bots rodando", runningBots.size());

        for (TradingBot bot : runningBots) {
            try {
                log.info("🤖 Executando bot: {} | Estratégia: {} | Moeda: {}",
                        bot.getName(), bot.getStrategy(), bot.getCoinSymbol());
                executeBot(bot);
            } catch (Exception e) {
                log.error("Erro ao executar bot {}: {}", bot.getId(), e.getMessage());
                bot.setStatus(TradingBot.BotStatus.ERROR);
                botRepository.save(bot);
            }
        }
    }

    /**
     * Executa a lógica do bot baseado na estratégia
     */
    private void executeBot(TradingBot bot) {
        switch (bot.getStrategy()) {
            case GRID_TRADING:
                executeGridTrading(bot);
                break;
            case DCA:
                executeDCA(bot);
                break;
            case STOP_LOSS:
                executeStopLossTakeProfit(bot);
                break;
            default:
                log.warn("Estratégia {} não implementada", bot.getStrategy());
        }
    }

    /**
     * ================================================
     * GRID TRADING STRATEGY - VERSÃO CORRIGIDA
     * ================================================
     */
    private void executeGridTrading(TradingBot bot) {
        log.info("📈 Executando Grid Trading para {}", bot.getCoinSymbol());

        String coinId = mapSymbolToCoinId(bot.getCoinSymbol());
        Optional<CryptoCurrency> cryptoOpt = cryptoService.getCryptoByCoinId(coinId);

        if (cryptoOpt.isEmpty()) {
            log.warn("⚠️ Crypto {} não encontrada", bot.getCoinSymbol());
            return;
        }

        CryptoCurrency crypto = cryptoOpt.get();
        BigDecimal currentPrice = crypto.getCurrentPrice();

        log.info("💰 Preço atual de {}: ${}", bot.getCoinSymbol(), currentPrice);
        log.info("📊 Range do Grid: ${} - ${}", bot.getGridLowerPrice(), bot.getGridUpperPrice());

        if (currentPrice.compareTo(bot.getGridLowerPrice()) < 0) {
            log.warn("⚠️ Preço ${} está ABAIXO do range mínimo ${}", currentPrice, bot.getGridLowerPrice());
            return;
        }

        if (currentPrice.compareTo(bot.getGridUpperPrice()) > 0) {
            log.warn("⚠️ Preço ${} está ACIMA do range máximo ${}", currentPrice, bot.getGridUpperPrice());
            return;
        }

        BigDecimal gridSize = bot.getGridUpperPrice()
                .subtract(bot.getGridLowerPrice())
                .divide(BigDecimal.valueOf(bot.getGridLevels()), 8, RoundingMode.HALF_UP);

        BigDecimal priceFromLower = currentPrice.subtract(bot.getGridLowerPrice());
        int currentGridLevel = priceFromLower.divide(gridSize, 0, RoundingMode.DOWN).intValue();

        log.info("🎯 Nível do Grid atual: {} de {}", currentGridLevel, bot.getGridLevels());

        if (currentGridLevel < bot.getGridLevels() / 3) {
            log.info("🟢 Zona de COMPRA detectada!");
            executeTrade(bot, crypto, BotTrade.TradeSide.BUY, "Grid Trading - Zona de compra");
        } else if (currentGridLevel > (bot.getGridLevels() * 2 / 3)) {
            log.info("🔴 Zona de VENDA detectada!");
            executeTrade(bot, crypto, BotTrade.TradeSide.SELL, "Grid Trading - Zona de venda");
        } else {
            log.info("⚪ Preço no meio do grid - aguardando");
        }
    }

    /**
     * ================================================
     * DCA STRATEGY - VERSÃO CORRIGIDA
     * ================================================
     */
    private void executeDCA(TradingBot bot) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastExecution = bot.getLastDcaExecution();

        if (lastExecution != null) {
            long minutesSinceLastExecution = Duration.between(lastExecution, now).toMinutes();
            if (minutesSinceLastExecution < bot.getDcaIntervalMinutes()) {
                log.debug("⏳ DCA: Ainda faltam {} minutos para próxima execução",
                        bot.getDcaIntervalMinutes() - minutesSinceLastExecution);
                return;
            }
        }

        log.info("💰 Executando DCA para {}", bot.getCoinSymbol());

        String coinId = mapSymbolToCoinId(bot.getCoinSymbol());
        Optional<CryptoCurrency> cryptoOpt = cryptoService.getCryptoByCoinId(coinId);

        if (cryptoOpt.isEmpty()) {
            log.warn("⚠️ Crypto {} não encontrada", bot.getCoinSymbol());
            return;
        }

        CryptoCurrency crypto = cryptoOpt.get();
        BigDecimal quantity = bot.getDcaAmount().divide(crypto.getCurrentPrice(), 8, RoundingMode.HALF_UP);

        BotTrade trade = BotTrade.builder()
                .bot(bot)
                .coinSymbol(bot.getCoinSymbol())
                .side(BotTrade.TradeSide.BUY)
                .price(crypto.getCurrentPrice())
                .quantity(quantity)
                .totalValue(bot.getDcaAmount())
                .isSimulation(bot.getIsSimulation())
                .reason("DCA: Compra periódica de $" + bot.getDcaAmount())
                .build();

        tradeRepository.save(trade);

        bot.setTotalTrades(bot.getTotalTrades() + 1);
        bot.setLastDcaExecution(now);
        botRepository.save(bot);

        log.info("✅ DCA executado com sucesso!");
    }

    /**
     * ================================================
     * STOP LOSS / TAKE PROFIT - VERSÃO CORRIGIDA
     * ================================================
     */
    private void executeStopLossTakeProfit(TradingBot bot) {
        if (bot.getEntryPrice() == null) {
            log.warn("⚠️ Preço de entrada não definido para bot: {}", bot.getName());
            return;
        }

        String coinId = mapSymbolToCoinId(bot.getCoinSymbol());
        Optional<CryptoCurrency> cryptoOpt = cryptoService.getCryptoByCoinId(coinId);

        if (cryptoOpt.isEmpty()) {
            log.warn("⚠️ Crypto {} não encontrada", bot.getCoinSymbol());
            return;
        }

        CryptoCurrency crypto = cryptoOpt.get();
        BigDecimal currentPrice = crypto.getCurrentPrice();
        BigDecimal entryPrice = bot.getEntryPrice();

        BigDecimal priceChange = currentPrice.subtract(entryPrice)
                .divide(entryPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        log.info("📊 Preço de entrada: ${} | Atual: ${} | Variação: {}%", entryPrice, currentPrice, priceChange);

        if (bot.getStopLossPercent() != null) {
            BigDecimal stopLossThreshold = bot.getStopLossPercent().negate();
            if (priceChange.compareTo(stopLossThreshold) <= 0) {
                log.warn("🔴 STOP LOSS ACIONADO! Queda de {}%", priceChange.abs());
                executeTrade(bot, crypto, BotTrade.TradeSide.SELL,
                        String.format("Stop Loss acionado: %.2f%% de queda", priceChange.abs()));
                bot.setStatus(TradingBot.BotStatus.STOPPED);
                botRepository.save(bot);
                return;
            }
        }

        if (bot.getTakeProfitPercent() != null && priceChange.compareTo(bot.getTakeProfitPercent()) >= 0) {
            log.info("🟢 TAKE PROFIT ACIONADO! Lucro de {}%", priceChange);
            executeTrade(bot, crypto, BotTrade.TradeSide.SELL,
                    String.format("Take Profit acionado: %.2f%% de lucro", priceChange));
            bot.setStatus(TradingBot.BotStatus.STOPPED);
            botRepository.save(bot);
        }
    }

    /**
     * Executa compra/venda genérica
     */
    private void executeTrade(TradingBot bot, CryptoCurrency crypto, BotTrade.TradeSide side, String reason) {
        BigDecimal price = crypto.getCurrentPrice();
        BigDecimal quantity = bot.getAmountPerGrid();

        if (side == BotTrade.TradeSide.BUY) {
            executeBuy(bot, price, quantity, reason);
        } else {
            executeSell(bot, price, quantity, reason);
        }
    }

    /** Executa compra */
    private void executeBuy(TradingBot bot, BigDecimal price, BigDecimal quantity, String reason) {
        BotTrade trade = BotTrade.builder()
                .bot(bot)
                .coinSymbol(bot.getCoinSymbol())
                .side(BotTrade.TradeSide.BUY)
                .price(price)
                .quantity(quantity)
                .isSimulation(bot.getIsSimulation())
                .reason(reason)
                .build();

        tradeRepository.save(trade);
        bot.setTotalTrades(bot.getTotalTrades() + 1);
        botRepository.save(bot);
        log.info("🟢 Bot {} COMPROU {} {} @ {} ({})", bot.getName(), quantity, bot.getCoinSymbol(), price, reason);
    }

    /** Executa venda */
    private void executeSell(TradingBot bot, BigDecimal price, BigDecimal quantity, String reason) {
        List<BotTrade> trades = tradeRepository.findByBotOrderByExecutedAtDesc(bot);
        BigDecimal profitLoss = BigDecimal.ZERO;

        Optional<BotTrade> lastBuy = trades.stream()
                .filter(t -> t.getSide() == BotTrade.TradeSide.BUY)
                .findFirst();

        if (lastBuy.isPresent()) {
            BigDecimal buyPrice = lastBuy.get().getPrice();
            profitLoss = price.subtract(buyPrice).multiply(quantity);
        }

        BotTrade trade = BotTrade.builder()
                .bot(bot)
                .coinSymbol(bot.getCoinSymbol())
                .side(BotTrade.TradeSide.SELL)
                .price(price)
                .quantity(quantity)
                .profitLoss(profitLoss)
                .isSimulation(bot.getIsSimulation())
                .reason(reason)
                .build();

        tradeRepository.save(trade);
        bot.setTotalTrades(bot.getTotalTrades() + 1);
        bot.setTotalProfitLoss(bot.getTotalProfitLoss().add(profitLoss));

        if (profitLoss.compareTo(BigDecimal.ZERO) > 0)
            bot.setWinningTrades(bot.getWinningTrades() + 1);
        else
            bot.setLosingTrades(bot.getLosingTrades() + 1);

        botRepository.save(bot);

        log.info("🔴 Bot {} VENDEU {} {} @ {} (P/L: {}) ({})",
                bot.getName(), quantity, bot.getCoinSymbol(), price, profitLoss, reason);
    }

    /** Buscar bot por ID e validar usuário */
    private TradingBot getBotByIdAndUser(Long botId, String username) {
        TradingBot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot não encontrado"));

        if (!bot.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Você não tem permissão para acessar este bot");
        }
        return bot;
    }

    /** Buscar bots do usuário */
    public List<TradingBot> getUserBots(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return botRepository.findByUser(user);
    }

    /** Buscar trades de um bot */
    public List<BotTrade> getBotTrades(String username, Long botId) {
        TradingBot bot = getBotByIdAndUser(botId, username);
        return tradeRepository.findByBotOrderByExecutedAtDesc(bot);
    }

    /** Deletar bot */
    @Transactional
    public void deleteBot(String username, Long botId) {
        TradingBot bot = getBotByIdAndUser(botId, username);

        if (bot.getStatus() == TradingBot.BotStatus.RUNNING) {
            throw new RuntimeException("Pare o bot antes de deletá-lo");
        }

        botRepository.delete(bot);
        log.info("Bot {} deletado", bot.getName());
    }

    /** Enviar alerta de bot */
    private void sendAlert(TradingBot bot, String subject, String message) {
        try {
            String email = bot.getUser().getEmail();
            if (email != null && !email.isEmpty()) {
                notificationService.sendEmailAlert(email, subject, message);
            }
        } catch (Exception e) {
            log.error("Erro ao enviar alerta: {}", e.getMessage());
        }
    }
}

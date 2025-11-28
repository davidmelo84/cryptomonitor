

package com.crypto.service;

import com.crypto.model.CryptoCurrency;
import com.crypto.model.*;
import com.crypto.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

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
        return symbolMap.getOrDefault(upperSymbol, symbol.toLowerCase());
    }


    @Transactional
    public TradingBot createBot(String username, TradingBot bot) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        bot.setUser(user);
        bot.setStatus(TradingBot.BotStatus.STOPPED);
        bot.setCreatedAt(LocalDateTime.now());

        return botRepository.save(bot);
    }


    @Transactional
    public void startBot(String username, Long botId) {
        TradingBot bot = getBotByIdAndUser(botId, username);

        if (bot.getStatus() == TradingBot.BotStatus.RUNNING)
            throw new RuntimeException("Bot já está rodando");

        bot.setStatus(TradingBot.BotStatus.RUNNING);
        bot.setStartedAt(LocalDateTime.now());

        String coinId = mapSymbolToCoinId(bot.getCoinSymbol());
        cryptoService.getCryptoByCoinId(coinId)
                .ifPresent(c -> bot.setEntryPrice(c.getCurrentPrice()));

        botRepository.save(bot);
    }


    @Transactional
    public void stopBot(String username, Long botId) {
        TradingBot bot = getBotByIdAndUser(botId, username);

        bot.setStatus(TradingBot.BotStatus.STOPPED);
        bot.setStoppedAt(LocalDateTime.now());

        botRepository.save(bot);
    }


    @Scheduled(fixedDelay = 60000)
    public void executeBots() {
        List<TradingBot> runningBots = botRepository.findByStatus(TradingBot.BotStatus.RUNNING);

        for (TradingBot bot : runningBots) {
            try {
                executeBot(bot);
            } catch (Exception e) {
                bot.setStatus(TradingBot.BotStatus.ERROR);
                botRepository.save(bot);
            }
        }
    }

    private void executeBot(TradingBot bot) {
        switch (bot.getStrategy()) {
            case GRID_TRADING -> executeGridTrading(bot);
            case DCA -> executeDCA(bot);
            case STOP_LOSS -> executeStopLossTakeProfit(bot);
            default -> log.warn("Estratégia {} não implementada", bot.getStrategy());
        }
    }


    private void executeGridTrading(TradingBot bot) {
        String coinId = mapSymbolToCoinId(bot.getCoinSymbol());
        Optional<CryptoCurrency> cryptoOpt = cryptoService.getCryptoByCoinId(coinId);

        if (cryptoOpt.isEmpty()) return;

        CryptoCurrency crypto = cryptoOpt.get();
        BigDecimal currentPrice = crypto.getCurrentPrice();

        if (currentPrice.compareTo(bot.getGridLowerPrice()) < 0) return;
        if (currentPrice.compareTo(bot.getGridUpperPrice()) > 0) return;

        BigDecimal gridSize = bot.getGridUpperPrice()
                .subtract(bot.getGridLowerPrice())
                .divide(BigDecimal.valueOf(bot.getGridLevels()), 8, RoundingMode.HALF_UP);

        BigDecimal priceFromLower = currentPrice.subtract(bot.getGridLowerPrice());
        int currentGridLevel = priceFromLower.divide(gridSize, 0, RoundingMode.DOWN).intValue();

        if (currentGridLevel < bot.getGridLevels() / 3) {
            executeTrade(bot, crypto, BotTrade.TradeSide.BBUY, "Grid Trading - Zona de compra");
        } else if (currentGridLevel > (bot.getGridLevels() * 2 / 3)) {
            executeTrade(bot, crypto, BotTrade.TradeSide.SELL, "Grid Trading - Zona de venda");
        }
    }


    private void executeDCA(TradingBot bot) {
        LocalDateTime now = LocalDateTime.now();

        if (bot.getLastDcaExecution() != null) {
            long minutes = Duration.between(bot.getLastDcaExecution(), now).toMinutes();
            if (minutes < bot.getDcaIntervalMinutes()) return;
        }

        String coinId = mapSymbolToCoinId(bot.getCoinSymbol());
        Optional<CryptoCurrency> cryptoOpt = cryptoService.getCryptoByCoinId(coinId);

        if (cryptoOpt.isEmpty()) return;

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
                .reason("DCA - Compra automática")
                .build();

        tradeRepository.save(trade);

        bot.setTotalTrades(bot.getTotalTrades() + 1);
        bot.setLastDcaExecution(now);
        botRepository.save(bot);
    }


    private void executeStopLossTakeProfit(TradingBot bot) {
        if (bot.getEntryPrice() == null) return;

        String coinId = mapSymbolToCoinId(bot.getCoinSymbol());
        Optional<CryptoCurrency> cryptoOpt = cryptoService.getCryptoByCoinId(coinId);

        if (cryptoOpt.isEmpty()) return;

        CryptoCurrency crypto = cryptoOpt.get();
        BigDecimal currentPrice = crypto.getCurrentPrice();

        BigDecimal pct =
                currentPrice.subtract(bot.getEntryPrice())
                        .divide(bot.getEntryPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        if (bot.getStopLossPercent() != null &&
                pct.compareTo(bot.getStopLossPercent().negate()) <= 0) {

            executeTrade(bot, crypto, BotTrade.TradeSide.SELL,
                    "Stop Loss acionado");
            bot.setStatus(TradingBot.BotStatus.STOPPED);
            botRepository.save(bot);
            return;
        }

        if (bot.getTakeProfitPercent() != null &&
                pct.compareTo(bot.getTakeProfitPercent()) >= 0) {

            executeTrade(bot, crypto, BotTrade.TradeSide.SELL,
                    "Take Profit acionado");
            bot.setStatus(TradingBot.BotStatus.STOPPED);
            botRepository.save(bot);
        }
    }

    private void executeTrade(TradingBot bot, CryptoCurrency crypto, BotTrade.TradeSide side, String reason) {
        BigDecimal price = crypto.getCurrentPrice();
        BigDecimal quantity = bot.getAmountPerGrid();

        if (side == BotTrade.TradeSide.BUY)
            executeBuy(bot, price, quantity, reason);
        else
            executeSell(bot, price, quantity, reason);
    }


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
    }

    private BigDecimal getAvailableBalance(TradingBot bot) {

        List<BotTrade> buys = tradeRepository.findByBotAndSideOrderByExecutedAtAsc(
                bot, BotTrade.TradeSide.BUY);

        List<BotTrade> sells = tradeRepository.findByBotAndSideOrderByExecutedAtAsc(
                bot, BotTrade.TradeSide.SELL);

        BigDecimal totalBuy = buys.stream()
                .map(BotTrade::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSell = sells.stream()
                .map(BotTrade::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalBuy.subtract(totalSell);
    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    private void executeSell(TradingBot bot, BigDecimal price, BigDecimal quantity, String reason) {

        BigDecimal available = getAvailableBalance(bot);

        if (available.compareTo(quantity) < 0) {
            log.warn("VENDA BLOQUEADA — saldo insuficiente. Disp: {} | Req: {}", available, quantity);
            return;
        }

        List<BotTrade> buys = tradeRepository.findByBotAndSideOrderByExecutedAtAsc(
                bot, BotTrade.TradeSide.BUY);

        BigDecimal remaining = quantity;
        BigDecimal totalProfit = BigDecimal.ZERO;

        for (BotTrade buy : buys) {

            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal soldQty = buy.getSoldQuantity() == null ? BigDecimal.ZERO : buy.getSoldQuantity();
            BigDecimal availableFromBuy = buy.getQuantity().subtract(soldQty);

            if (availableFromBuy.compareTo(BigDecimal.ZERO) <= 0)
                continue;

            BigDecimal sellQty = availableFromBuy.min(remaining);

            BigDecimal profit = price.subtract(buy.getPrice())
                    .multiply(sellQty);

            buy.setSoldQuantity(soldQty.add(sellQty));
            tradeRepository.save(buy);

            totalProfit = totalProfit.add(profit);
            remaining = remaining.subtract(sellQty);
        }

        BigDecimal executedQty = quantity.subtract(remaining);

        BotTrade trade = BotTrade.builder()
                .bot(bot)
                .coinSymbol(bot.getCoinSymbol())
                .side(BotTrade.TradeSide.SELL)
                .price(price)
                .quantity(executedQty)
                .profitLoss(totalProfit)
                .isSimulation(bot.getIsSimulation())
                .reason(reason)
                .build();

        tradeRepository.save(trade);

        bot.setTotalTrades(bot.getTotalTrades() + 1);
        bot.setTotalProfitLoss(bot.getTotalProfitLoss().add(totalProfit));

        if (totalProfit.compareTo(BigDecimal.ZERO) > 0)
            bot.setWinningTrades(bot.getWinningTrades() + 1);
        else if (totalProfit.compareTo(BigDecimal.ZERO) < 0)
            bot.setLosingTrades(bot.getLosingTrades() + 1);

        botRepository.save(bot);
    }

    private TradingBot getBotByIdAndUser(Long botId, String username) {
        TradingBot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot não encontrado"));

        if (!bot.getUser().getUsername().equals(username))
            throw new RuntimeException("Acesso negado");

        return bot;
    }

    public List<TradingBot> getUserBots(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return botRepository.findByUser(user);
    }

    public List<BotTrade> getBotTrades(String username, Long botId) {
        TradingBot bot = getBotByIdAndUser(botId, username);
        return tradeRepository.findByBotOrderByExecutedAtDesc(bot);
    }

    @Transactional
    public void deleteBot(String username, Long botId) {
        TradingBot bot = getBotByIdAndUser(botId, username);

        if (bot.getStatus() == TradingBot.BotStatus.RUNNING)
            throw new RuntimeException("Pare o bot antes de deletá-lo");

        botRepository.delete(bot);
    }

    private void sendAlert(TradingBot bot, String subject, String message) {
        try {
            if (bot.getUser().getEmail() != null) {
                notificationService.sendEmailAlert(bot.getUser().getEmail(), subject, message);
            }
        } catch (Exception ignored) {}
    }
}

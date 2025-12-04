-- ============================================
-- V17__add_critical_composite_indexes.sql
-- Índices compostos para queries mais frequentes
-- ============================================

-- ✅ Portfolio: user_id + coin_symbol (COVERING INDEX)
CREATE INDEX IF NOT EXISTS idx_portfolio_user_coin_covering
    ON portfolio(user_id, coin_symbol, quantity, average_buy_price, total_invested)
    WHERE quantity > 0;

-- ✅ Transactions: user_id + date DESC (ORDER BY otimizado)
CREATE INDEX IF NOT EXISTS idx_transactions_user_date_desc_covering
    ON transactions(user_id, transaction_date DESC)
    INCLUDE (coin_symbol, type, quantity, price_per_unit, total_value);

-- ✅ Alert Rules: email + active + symbol (busca mais comum)
CREATE INDEX IF NOT EXISTS idx_alert_rules_email_active_symbol_covering
    ON alert_rules(notification_email, is_active, coin_symbol)
    WHERE is_active = true
    INCLUDE (alert_type, threshold_value);

-- ✅ Bot Trades: FIFO lookup otimizado
CREATE INDEX IF NOT EXISTS idx_bot_trades_fifo_covering
    ON bot_trades(bot_id, side, executed_at ASC)
    WHERE side = 'BUY'
    INCLUDE (quantity, sold_quantity, price);

-- ✅ Verification Tokens: code lookup com verificação
CREATE INDEX IF NOT EXISTS idx_verification_tokens_code_active
    ON verification_tokens(code, verified, expiry_date)
    WHERE verified = false;

COMMENT ON INDEX idx_portfolio_user_coin_covering IS
    'Covering index para getPortfolio - evita lookup na tabela';

COMMENT ON INDEX idx_transactions_user_date_desc_covering IS
    'Covering index para listagem de transações com ORDER BY';
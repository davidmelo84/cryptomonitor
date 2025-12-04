-- ============================================
-- V17__add_critical_composite_indexes.sql
-- Índices compostos para queries mais frequentes
-- CORRIGIDO: Sem INCLUDE para compatibilidade
-- ============================================

-- ✅ Portfolio: user_id + coin_symbol (sem INCLUDE)
CREATE INDEX IF NOT EXISTS idx_portfolio_user_coin_covering
    ON portfolio(user_id, coin_symbol, quantity, average_buy_price, total_invested)
    WHERE quantity > 0;

-- ✅ Transactions: user_id + date DESC (sem INCLUDE)
CREATE INDEX IF NOT EXISTS idx_transactions_user_date_desc_covering
    ON transactions(user_id, transaction_date DESC, coin_symbol, type, quantity, price_per_unit, total_value);

-- ✅ Alert Rules: email + active + symbol (sem INCLUDE)
CREATE INDEX IF NOT EXISTS idx_alert_rules_email_active_symbol_covering
    ON alert_rules(notification_email, is_active, coin_symbol, alert_type, threshold_value)
    WHERE is_active = true;

-- ✅ Bot Trades: FIFO lookup otimizado (sem INCLUDE)
CREATE INDEX IF NOT EXISTS idx_bot_trades_fifo_covering
    ON bot_trades(bot_id, side, executed_at ASC, quantity, sold_quantity, price)
    WHERE side = 'BUY';

-- ✅ Verification Tokens: code lookup com verificação (sem INCLUDE)
CREATE INDEX IF NOT EXISTS idx_verification_tokens_code_active
    ON verification_tokens(code, verified, expiry_date)
    WHERE verified = false;

-- ✅ Comentários para documentação
COMMENT ON INDEX idx_portfolio_user_coin_covering IS
    'Índice composto para getPortfolio - compatível com todas versões PostgreSQL';

COMMENT ON INDEX idx_transactions_user_date_desc_covering IS
    'Índice composto para listagem de transações com ORDER BY';

COMMENT ON INDEX idx_alert_rules_email_active_symbol_covering IS
    'Índice composto para busca de alertas ativos por email e símbolo';

COMMENT ON INDEX idx_bot_trades_fifo_covering IS
    'Índice para cálculo FIFO de vendas de bot trades';

COMMENT ON INDEX idx_verification_tokens_code_active IS
    'Índice para validação rápida de códigos de verificação';
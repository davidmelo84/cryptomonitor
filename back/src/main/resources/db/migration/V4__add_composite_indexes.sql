-- back/src/main/resources/db/migration/V4__add_composite_indexes.sql

-- ✅ Índice composto para busca de alertas por email + símbolo
CREATE INDEX idx_alert_rules_email_symbol_active
    ON alert_rules(notification_email, coin_symbol, is_active);

-- ✅ Índice para portfolio por usuário
CREATE INDEX idx_portfolio_user_symbol
    ON portfolio(user_id, coin_symbol);

-- ✅ Índice para transações por usuário e data
CREATE INDEX idx_transactions_user_date
    ON transactions(user_id, transaction_date DESC);

-- ✅ Índice para bots ativos
CREATE INDEX idx_bots_status_user
    ON trading_bots(status, user_id)
    WHERE status = 'RUNNING';
-- ================================================================
-- V4__add_composite_indexes.sql
-- Ajuste completo: adiciona colunas faltantes e cria índices otimizados
-- ================================================================

-- 🩵 Corrigir colunas faltantes antes dos índices
ALTER TABLE alert_rules
    ADD COLUMN IF NOT EXISTS notification_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- ✅ Índice composto para busca de alertas por símbolo + ativo
CREATE INDEX IF NOT EXISTS idx_alert_rules_email_symbol_active
    ON alert_rules(coin_symbol, is_active);

-- ✅ Índices complementares para segurança e desempenho
CREATE INDEX IF NOT EXISTS idx_alert_rules_user_id ON alert_rules(user_id);
CREATE INDEX IF NOT EXISTS idx_alert_rules_coin_symbol ON alert_rules(coin_symbol);

-- ✅ Índice para portfolio por usuário
CREATE INDEX IF NOT EXISTS idx_portfolio_user_symbol
    ON portfolio(user_id, coin_symbol);

-- ✅ Índice para transações por usuário e data
CREATE INDEX IF NOT EXISTS idx_transactions_user_date
    ON transactions(user_id, transaction_date DESC);

-- ✅ Índice para bots ativos
CREATE INDEX IF NOT EXISTS idx_bots_status_user
    ON trading_bots(status, user_id)
    WHERE status = 'RUNNING';

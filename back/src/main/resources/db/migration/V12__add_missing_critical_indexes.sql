-- ============================================
-- V12__add_missing_critical_indexes.sql
-- Índices críticos para performance
-- ============================================

-- ✅ Busca de alertas por email + status
CREATE INDEX IF NOT EXISTS idx_alert_rules_email_active_compound
    ON alert_rules(notification_email, is_active, coin_symbol)
    WHERE is_active = true;

-- ✅ Busca de usuários não verificados (UserCleanupService)
CREATE INDEX IF NOT EXISTS idx_users_enabled_email
    ON users(enabled, email)
    WHERE enabled = false;

-- ✅ Busca de tokens por código (verificação)
CREATE INDEX IF NOT EXISTS idx_verification_code_verified
    ON verification_tokens(code, verified)
    WHERE verified = false;

-- ✅ Busca de portfolio por usuário (acesso frequente)
CREATE INDEX IF NOT EXISTS idx_portfolio_user_quantity
    ON portfolio(user_id, coin_symbol, quantity)
    WHERE quantity > 0;

-- ✅ Histórico de transações por data
CREATE INDEX IF NOT EXISTS idx_transactions_user_date_type
    ON transactions(user_id, transaction_date DESC, type);

-- ✅ Bots ativos para scheduler
CREATE INDEX IF NOT EXISTS idx_trading_bots_status_started
    ON trading_bots(status, started_at DESC)
    WHERE status = 'RUNNING';

-- ✅ Comentários para documentação
COMMENT ON INDEX idx_alert_rules_email_active_compound IS
    'Índice composto para busca de alertas ativos por email';

COMMENT ON INDEX idx_users_enabled_email IS
    'Otimiza busca de usuários não verificados';

COMMENT ON INDEX idx_verification_code_verified IS
    'Acelera validação de códigos de verificação';
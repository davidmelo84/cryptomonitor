-- ============================================
-- V8__add_missing_indexes.sql (VERSÃO CORRIGIDA)
-- Adiciona índices para melhorar performance
-- ============================================

-- ✅ Índice para verificar tokens não verificados (UserCleanupService)
CREATE INDEX IF NOT EXISTS idx_verification_tokens_verified_expiry
    ON verification_tokens(verified, expiry_date)
    WHERE verified = false;

-- ✅ Índice para buscar usuários não verificados antigos
CREATE INDEX IF NOT EXISTS idx_users_enabled_created
    ON users(enabled, created_at)
    WHERE enabled = false;

-- ✅ Índice para bot trades por data (performance no dashboard)
-- IMPORTANTE: Só cria se a tabela bot_trades existir
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'bot_trades') THEN
        CREATE INDEX IF NOT EXISTS idx_bot_trades_bot_executed
            ON bot_trades(bot_id, executed_at DESC);
        RAISE NOTICE '✅ Índice bot_trades criado';
    ELSE
        RAISE NOTICE '⏭️ Pulando índice bot_trades (tabela não existe ainda)';
    END IF;
END $$;

-- ✅ Índice para cryptocurrencies por símbolo (usado frequentemente)
CREATE INDEX IF NOT EXISTS idx_cryptocurrencies_symbol_updated
    ON cryptocurrencies(symbol, last_updated DESC);

-- ✅ Índice parcial para alertas ativos (WHERE clause comum)
CREATE INDEX IF NOT EXISTS idx_alert_rules_active_symbol
    ON alert_rules(is_active, coin_symbol)
    WHERE is_active = true;

-- ✅ Comentários para documentação
COMMENT ON INDEX idx_verification_tokens_verified_expiry IS
    'Usado pelo UserCleanupService para encontrar tokens não verificados expirados';

COMMENT ON INDEX idx_users_enabled_created IS
    'Usado pelo UserCleanupService para encontrar contas antigas não verificadas';

COMMENT ON INDEX idx_cryptocurrencies_symbol_updated IS
    'Otimiza buscas por símbolo com dados mais recentes';

COMMENT ON INDEX idx_alert_rules_active_symbol IS
    'Índice parcial para alertas ativos (reduz tamanho do índice)';

-- ✅ Log de sucesso
DO $$
BEGIN
    RAISE NOTICE '✅ Migration V8 concluída (versão corrigida)';
END $$;
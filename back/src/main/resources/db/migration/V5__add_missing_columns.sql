-- Corrigir colunas faltantes antes dos índices da V4
ALTER TABLE alert_rules
    ADD COLUMN IF NOT EXISTS notification_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- Índices complementares para segurança
CREATE INDEX IF NOT EXISTS idx_alert_rules_user_id ON alert_rules(user_id);
CREATE INDEX IF NOT EXISTS idx_alert_rules_coin_symbol ON alert_rules(coin_symbol);

-- ============================================
-- V6__alter_target_price_nullable.sql
-- Torna target_price opcional para alertas que não usam valor-alvo
-- ============================================

-- 1) Tornar a coluna nullable (idempotente)
ALTER TABLE IF EXISTS alert_rules
    ALTER COLUMN target_price DROP NOT NULL;

-- 2) Criar comentário (idempotente)
COMMENT ON COLUMN alert_rules.target_price IS
    'Preço alvo para alertas do tipo PRICE_INCREASE/PRICE_DECREASE. Pode ser NULL para alertas como PERCENT_CHANGE_24H.';

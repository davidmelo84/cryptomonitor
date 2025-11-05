MIGRATION: Tornar target_price opcional
-- Criado em: 2025-11-05
-- Motivo: Alertas PERCENT_CHANGE_24H não usam target_price
-- ============================================

ALTER TABLE alert_rules
    ALTER COLUMN target_price DROP NOT NULL;

-- Adicionar comentário para documentação
COMMENT ON COLUMN alert_rules.target_price IS
'Preço alvo para alertas do tipo PRICE_THRESHOLD.
Pode ser NULL para alertas do tipo PERCENT_CHANGE_24H.';
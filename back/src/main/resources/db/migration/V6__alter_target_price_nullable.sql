-- ============================================
-- V6__alter_target_price_nullable.sql
-- MIGRATION: Tornar target_price opcional
-- Criado em: 2025-11-05
-- Motivo: Alertas PERCENT_CHANGE_24H não usam target_price
-- ============================================

-- ✅ Verificar se a coluna existe antes de alterar
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name='alert_rules'
        AND column_name='target_price'
    ) THEN
        -- Tornar target_price nullable
ALTER TABLE alert_rules
    ALTER COLUMN target_price DROP NOT NULL;

-- Adicionar comentário para documentação
COMMENT ON COLUMN alert_rules.target_price IS
        'Preço alvo para alertas do tipo PRICE_INCREASE/PRICE_DECREASE. Pode ser NULL para alertas do tipo PERCENT_CHANGE_24H.';

        RAISE NOTICE 'target_price alterado para nullable com sucesso';
ELSE
        RAISE NOTICE 'Coluna target_price não existe, pulando migration';
END IF;
END $$;
-- ============================================
-- V14__force_target_price_fix.sql
-- CORREÇÃO DEFINITIVA: Garante target_price nullable
-- ============================================

-- Forçar DROP NOT NULL de forma idempotente
DO $$
BEGIN
    -- Verificar se existe a constraint NOT NULL
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'alert_rules'
          AND column_name = 'target_price'
          AND is_nullable = 'NO'
    ) THEN
        -- Remover NOT NULL
        ALTER TABLE alert_rules
        ALTER COLUMN target_price DROP NOT NULL;

        RAISE NOTICE '✅ target_price agora é nullable';
    ELSE
        RAISE NOTICE 'ℹ️  target_price já era nullable';
    END IF;
END $$;

-- Garantir que valores NULL existentes sejam mantidos
UPDATE alert_rules
SET target_price = NULL
WHERE alert_type = 'PERCENT_CHANGE_24H'
  AND target_price IS NOT NULL;

-- Comentário explicativo
COMMENT ON COLUMN alert_rules.target_price IS
'Preço alvo para PRICE_INCREASE/PRICE_DECREASE. NULL para PERCENT_CHANGE_24H.';

-- Log de sucesso
DO $$
BEGIN
    RAISE NOTICE '✅ Migration V14 concluída com sucesso';
END $$;
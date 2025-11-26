-- ============================================
-- V13__fix_target_price_nullable.sql
-- Correção: Torna target_price opcional (IDEMPOTENTE)
-- ============================================

-- Verificar se a coluna existe antes de alterar
DO $$
BEGIN
    -- Tornar nullable se ainda não for
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'alert_rules'
          AND column_name = 'target_price'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE alert_rules
        ALTER COLUMN target_price DROP NOT NULL;

        RAISE NOTICE 'target_price agora é nullable';
    ELSE
        RAISE NOTICE 'target_price já era nullable';
    END IF;
END $$;

-- Adicionar comentário (idempotente)
COMMENT ON COLUMN alert_rules.target_price IS
    'Preço alvo para PRICE_INCREASE/PRICE_DECREASE. NULL para PERCENT_CHANGE_24H';
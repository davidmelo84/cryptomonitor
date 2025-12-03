-- ============================================
-- V16__fix_bot_trades_table.sql
-- CORREÇÃO CRÍTICA: Garante que bot_trades existe
-- ============================================

-- 1️⃣ Criar tabela bot_trades se não existir
CREATE TABLE IF NOT EXISTS bot_trades (
    id BIGSERIAL PRIMARY KEY,
    bot_id BIGINT NOT NULL,
    coin_symbol VARCHAR(50) NOT NULL,
    side VARCHAR(10) NOT NULL CHECK (side IN ('BUY', 'SELL', 'BBUY')),
    price NUMERIC(19,8) NOT NULL,
    quantity NUMERIC(19,8) NOT NULL,
    sold_quantity NUMERIC(19,8) DEFAULT 0,
    total_value NUMERIC(19,2) NOT NULL,
    profit_loss NUMERIC(19,2),
    is_simulation BOOLEAN NOT NULL DEFAULT TRUE,
    reason VARCHAR(255),
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2️⃣ Adicionar constraint de FK se trading_bots existir
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'trading_bots') THEN
        -- Remover constraint antiga se existir
        ALTER TABLE bot_trades DROP CONSTRAINT IF EXISTS bot_trades_bot_id_fkey;

        -- Adicionar constraint nova
        ALTER TABLE bot_trades
        ADD CONSTRAINT bot_trades_bot_id_fkey
        FOREIGN KEY (bot_id)
        REFERENCES trading_bots(id)
        ON DELETE CASCADE;

        RAISE NOTICE '✅ FK bot_trades -> trading_bots criada';
    ELSE
        RAISE WARNING '⚠️ Tabela trading_bots não existe ainda';
    END IF;
END $$;

-- 3️⃣ Criar índices de forma idempotente
CREATE INDEX IF NOT EXISTS idx_bot_trades_bot_executed
    ON bot_trades(bot_id, executed_at DESC);

CREATE INDEX IF NOT EXISTS idx_bot_trades_bot_side_executed
    ON bot_trades(bot_id, side, executed_at ASC)
    WHERE side = 'BUY';

-- 4️⃣ Adicionar coluna sold_quantity se não existir
ALTER TABLE bot_trades
    ADD COLUMN IF NOT EXISTS sold_quantity NUMERIC(19,8) DEFAULT 0;

-- 5️⃣ Comentários
COMMENT ON TABLE bot_trades IS
    'Registros de trades executados pelos bots (simulação ou real)';

COMMENT ON COLUMN bot_trades.sold_quantity IS
    'Quantidade já vendida desta compra (controle FIFO)';

-- 6️⃣ Log de sucesso
DO $$
BEGIN
    RAISE NOTICE '✅ Migration V16 concluída - bot_trades garantida';
END $$;
-- V11__add_sold_quantity_to_bot_trades.sql
-- Corrige sold_quantity e índice FIFO sem quebrar em runtime

ALTER TABLE bot_trades
    ADD COLUMN IF NOT EXISTS sold_quantity NUMERIC(19, 8) DEFAULT 0;

COMMENT ON COLUMN bot_trades.sold_quantity IS
    'Quantidade já vendida desta compra (controle FIFO)';

-- Detecta automaticamente se 'side' é enum ou texto
DO $$
BEGIN
    -- Se for ENUM, cria índice usando cast pro ENUM
    IF EXISTS (
        SELECT 1
        FROM pg_type
        WHERE typname = 'bot_side_enum'
    ) THEN
        CREATE INDEX IF NOT EXISTS idx_bot_trades_fifo
            ON bot_trades(bot_id, side, executed_at)
            WHERE side = 'BUY'::bot_side_enum;

    ELSE
        -- Se for VARCHAR/TEXT
        CREATE INDEX IF NOT EXISTS idx_bot_trades_fifo
            ON bot_trades(bot_id, side, executed_at)
            WHERE side = 'BUY';
    END IF;
END $$;

-- V11__add_sold_quantity_to_bot_trades.sql
-- Adiciona controle FIFO para vendas

ALTER TABLE bot_trades
    ADD COLUMN IF NOT EXISTS sold_quantity NUMERIC(19, 8) DEFAULT 0;

COMMENT ON COLUMN bot_trades.sold_quantity IS
    'Quantidade já vendida desta compra (controle FIFO)';

-- Índice para otimizar buscas FIFO
CREATE INDEX IF NOT EXISTS idx_bot_trades_bot_side_executed
    ON bot_trades(bot_id, side, executed_at ASC)
    WHERE side = 'BUY';
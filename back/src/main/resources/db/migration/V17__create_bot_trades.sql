-- V17__create_bot_trades.sql
-- Cria a tabela bot_trades com suporte ao controle FIFO

CREATE TABLE IF NOT EXISTS bot_trades (
    id BIGSERIAL PRIMARY KEY,
    bot_id BIGINT NOT NULL,
    side VARCHAR(10) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL,
    price NUMERIC(19, 8) NOT NULL,
    total NUMERIC(19, 8) NOT NULL,
    executed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),

    -- Controle FIFO
    sold_quantity NUMERIC(19, 8) DEFAULT 0
);

COMMENT ON TABLE bot_trades IS 'Registros de operações de compra/venda executadas por bots';
COMMENT ON COLUMN bot_trades.bot_id IS 'ID do bot que executou a operação';
COMMENT ON COLUMN bot_trades.side IS 'BUY ou SELL';
COMMENT ON COLUMN bot_trades.quantity IS 'Quantidade comprada/vendida';
COMMENT ON COLUMN bot_trades.price IS 'Preço unitário no momento da operação';
COMMENT ON COLUMN bot_trades.total IS 'Valor total da operação';
COMMENT ON COLUMN bot_trades.executed_at IS 'Momento em que a ordem foi executada';
COMMENT ON COLUMN bot_trades.sold_quantity IS 'Quantidade já vendida desta compra (controle FIFO)';

-- Índice FIFO para buscas rápidas de compras ordenadas
CREATE INDEX IF NOT EXISTS idx_bot_trades_bot_side_executed
    ON bot_trades (bot_id, side, executed_at ASC)
    WHERE side = 'BUY';

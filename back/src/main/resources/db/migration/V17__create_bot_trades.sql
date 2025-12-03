-- V17__create_bot_trades.sql
-- Criação completa da tabela bot_trades + colunas FIFO + índices

CREATE TABLE IF NOT EXISTS bot_trades (
    id BIGSERIAL PRIMARY KEY,
    bot_id BIGINT NOT NULL,
    coin_symbol VARCHAR(50) NOT NULL,
    side VARCHAR(10) NOT NULL,
    price NUMERIC(19, 8) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL,
    sold_quantity NUMERIC(19, 8) DEFAULT 0,
    total_value NUMERIC(19, 2) NOT NULL,
    profit_loss NUMERIC(19, 2),
    is_simulation BOOLEAN NOT NULL DEFAULT TRUE,
    reason VARCHAR(255),
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN bot_trades.sold_quantity IS
    'Quantidade já vendida desta compra (controle FIFO)';

-- Índice FIFO para buscas ordenadas
CREATE INDEX IF NOT EXISTS idx_bot_trades_bot_side_executed
    ON bot_trades (bot_id, side, executed_at ASC)
    WHERE side = 'BUY';

-- Índice usado em SELECTs por bot e data
CREATE INDEX IF NOT EXISTS idx_bot_trades_bot_executed
    ON bot_trades(bot_id, executed_at DESC);

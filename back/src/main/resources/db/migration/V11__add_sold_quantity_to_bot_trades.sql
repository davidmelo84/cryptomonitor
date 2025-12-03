-- V11__add_sold_quantity_to_bot_trades.sql

-- 1. CRIA A TABELA DE TRADES SE ELA NÃO EXISTIR (Salva o deploy pois ela estava faltando)
CREATE TABLE IF NOT EXISTS bot_trades (
    id BIGSERIAL PRIMARY KEY,
    bot_id BIGINT NOT NULL REFERENCES trading_bots(id) ON DELETE CASCADE,
    side VARCHAR(50) NOT NULL, -- 'BUY' ou 'SELL'
    price NUMERIC(19, 8) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL,
    executed_at TIMESTAMP DEFAULT now(),
    created_at TIMESTAMP DEFAULT now()
);

-- 2. AGORA SEGUE O FLUXO ORIGINAL (Adiciona sold_quantity)
-- Usamos IF NOT EXISTS para garantir que não dê erro se criamos a tabela acima
ALTER TABLE bot_trades
    ADD COLUMN IF NOT EXISTS sold_quantity NUMERIC(19, 8) DEFAULT 0;

COMMENT ON COLUMN bot_trades.sold_quantity IS
    'Quantidade já vendida desta compra (controle FIFO)';

-- 3. CRIAÇÃO DE ÍNDICES INTELIGENTE
-- Detecta automaticamente se 'side' é enum ou texto para criar o índice correto
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
        -- Se for VARCHAR/TEXT (Caso mais provável agora que recriamos a tabela)
        CREATE INDEX IF NOT EXISTS idx_bot_trades_fifo
            ON bot_trades(bot_id, side, executed_at)
            WHERE side = 'BUY';
    END IF;
END $$;
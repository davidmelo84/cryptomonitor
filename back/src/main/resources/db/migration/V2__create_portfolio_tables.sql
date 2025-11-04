-- back/src/main/resources/db/migration/V2__create_portfolio_tables.sql

-- Tabela de Portfolio
CREATE TABLE IF NOT EXISTS portfolio (
                                         id SERIAL PRIMARY KEY,
                                         user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    coin_symbol VARCHAR(20) NOT NULL,
    coin_name VARCHAR(100) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL DEFAULT 0,
    average_buy_price NUMERIC(19, 8) NOT NULL DEFAULT 0,
    total_invested NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, coin_symbol)
    );

-- Tabela de Transações
CREATE TABLE IF NOT EXISTS transactions (
                                            id SERIAL PRIMARY KEY,
                                            user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    coin_symbol VARCHAR(20) NOT NULL,
    coin_name VARCHAR(100) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('BUY', 'SELL')),
    quantity NUMERIC(19, 8) NOT NULL,
    price_per_unit NUMERIC(19, 8) NOT NULL,
    total_value NUMERIC(19, 2) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_portfolio_user_id ON portfolio(user_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_coin_symbol ON portfolio(coin_symbol);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_coin_symbol ON transactions(coin_symbol);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date DESC);
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS cryptocurrencies (
                                                id SERIAL PRIMARY KEY,
                                                coin_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    current_price NUMERIC,
    market_cap NUMERIC,
    total_volume NUMERIC,
    last_updated TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS alert_rules (
                                           id SERIAL PRIMARY KEY,
                                           user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    coin_symbol VARCHAR(20) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    target_price NUMERIC NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

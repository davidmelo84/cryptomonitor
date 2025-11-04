CREATE TABLE trading_bots (
                              id BIGSERIAL PRIMARY KEY,
                              user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              name VARCHAR(255) NOT NULL,
                              coin_symbol VARCHAR(50) NOT NULL,
                              strategy VARCHAR(50) NOT NULL DEFAULT 'GRID_TRADING',
                              status VARCHAR(50) NOT NULL DEFAULT 'STOPPED',
                              is_simulation BOOLEAN NOT NULL DEFAULT TRUE,

    -- Grid Trading Parameters
                              grid_lower_price NUMERIC(19,8),
                              grid_upper_price NUMERIC(19,8),
                              grid_levels INT,
                              amount_per_grid NUMERIC(19,8),

    -- DCA Parameters
                              dca_amount NUMERIC(19,2),
                              dca_interval_minutes INT,
                              last_dca_execution TIMESTAMP,

    -- Stop Loss / Take Profit
                              stop_loss_percent NUMERIC(5,2),
                              take_profit_percent NUMERIC(5,2),
                              entry_price NUMERIC(19,8),

    -- Statistics
                              total_profit_loss NUMERIC(19,2) DEFAULT 0,
                              total_trades INT DEFAULT 0,
                              winning_trades INT DEFAULT 0,
                              losing_trades INT DEFAULT 0,

    -- Timestamps
                              created_at TIMESTAMP DEFAULT now(),
                              started_at TIMESTAMP,
                              stopped_at TIMESTAMP
);

-- Índices recomendados
CREATE INDEX idx_trading_bots_user_id ON trading_bots(user_id);
CREATE INDEX idx_trading_bots_status ON trading_bots(status);

-- 🔧 Otimização de queries do PortfolioService
-- Índice para acelerar busca por user_id + symbol

CREATE INDEX IF NOT EXISTS idx_portfolio_user_symbol_active
    ON portfolio(user_id, coin_symbol)
    WHERE quantity > 0;

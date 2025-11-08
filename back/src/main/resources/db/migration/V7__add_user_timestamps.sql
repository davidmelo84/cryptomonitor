-- ============================================
-- V7__add_user_timestamps.sql
-- Adiciona campos de timestamp na tabela users
-- ============================================

-- ✅ Adicionar colunas de timestamp
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- ✅ Definir valores padrão para registros existentes
UPDATE users
SET
    created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

-- ✅ Tornar created_at NOT NULL após preencher
ALTER TABLE users
    ALTER COLUMN created_at SET NOT NULL,
ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

-- ✅ Comentários para documentação
COMMENT ON COLUMN users.created_at IS 'Data/hora de criação da conta';
COMMENT ON COLUMN users.updated_at IS 'Data/hora da última atualização';

-- ✅ Índice para otimizar buscas por data de criação
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_enabled_created ON users(enabled, created_at)
    WHERE enabled = false;
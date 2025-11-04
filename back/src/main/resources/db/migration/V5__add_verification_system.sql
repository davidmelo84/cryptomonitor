-- ================================================================
-- V5__add_verification_system.sql
-- Adiciona sistema de verificação de e-mail por token e código
-- ================================================================

-- 🧩 Adicionar campo 'enabled' na tabela de usuários
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT FALSE;

-- 🧩 Criar tabela de tokens de verificação
CREATE TABLE IF NOT EXISTS verification_tokens (
                                                   id SERIAL PRIMARY KEY,
                                                   token VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(6) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 🧩 Índices para performance nas verificações
CREATE INDEX IF NOT EXISTS idx_verification_token ON verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_verification_code ON verification_tokens(code);
CREATE INDEX IF NOT EXISTS idx_verification_user ON verification_tokens(user_id);

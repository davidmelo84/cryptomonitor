-- back/src/main/resources/db/migration/V5__add_verification_system.sql

-- Adicionar campo 'enabled' na tabela users
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT FALSE;

-- Criar tabela de tokens de verificação
CREATE TABLE IF NOT EXISTS verification_tokens (
                                                   id SERIAL PRIMARY KEY,
                                                   token VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(6) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_verification_token ON verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_verification_code ON verification_tokens(code);
CREATE INDEX IF NOT EXISTS idx_verification_user ON verification_tokens(user_id);
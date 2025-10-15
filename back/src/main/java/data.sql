-- data.sql - Dados iniciais para a aplicação

-- Inserir algumas regras de alerta padrão
INSERT INTO alert_rules (coin_symbol, alert_type, threshold_value, time_period, is_active, notification_email) VALUES
                                                                                                                   ('BTC', 'PRICE_DECREASE', 5.00, 'TWENTY_FOUR_HOURS', true, 'seu-email@gmail.com'),
                                                                                                                   ('BTC', 'PRICE_INCREASE', 10.00, 'TWENTY_FOUR_HOURS', true, 'seu-email@gmail.com'),
                                                                                                                   ('ETH', 'PRICE_DECREASE', 5.00, 'TWENTY_FOUR_HOURS', true, 'seu-email@gmail.com'),
                                                                                                                   ('ETH', 'PRICE_INCREASE', 10.00, 'TWENTY_FOUR_HOURS', true, 'seu-email@gmail.com'),
                                                                                                                   ('ADA', 'PRICE_DECREASE', 7.00, 'TWENTY_FOUR_HOURS', true, 'seu-email@gmail.com'),
                                                                                                                   ('ADA', 'PRICE_INCREASE', 12.00, 'TWENTY_FOUR_HOURS', true, 'seu-email@gmail.com');

-- Índices para melhorar performance
CREATE INDEX IF NOT EXISTS idx_crypto_coin_id ON cryptocurrencies(coin_id);
CREATE INDEX IF NOT EXISTS idx_crypto_symbol ON cryptocurrencies(symbol);
CREATE INDEX IF NOT EXISTS idx_crypto_last_updated ON cryptocurrencies(last_updated);
CREATE INDEX IF NOT EXISTS idx_alert_rules_coin_symbol ON alert_rules(coin_symbol);
CREATE INDEX IF NOT EXISTS idx_alert_rules_active ON alert_rules(is_active);

-- Comentários sobre as configurações
--
-- IMPORTANTE: Antes de executar a aplicação, configure:
--
-- 1. Email (Gmail):
--    - Ative a verificação em duas etapas
--    - Crie uma "Senha de app" específica
--    - Configure as variáveis de ambiente:
--      MAIL_USERNAME=seu-email@gmail.com
--      MAIL_PASSWORD=sua-senha-de-app-do-gmail
--      NOTIFICATION_EMAIL=email-que-recebera-alertas@gmail.com
--
-- 2. Telegram (Opcional):
--    - Crie um bot via @BotFather
--    - Obtenha o token do bot
--    - Obtenha seu chat_id enviando uma mensagem para @userinfobot
--    - Configure as variáveis de ambiente:
--      TELEGRAM_ENABLED=true
--      TELEGRAM_BOT_TOKEN=seu-token-do-bot
--      TELEGRAM_CHAT_ID=seu-chat-id
--
-- 3. Para produção com PostgreSQL:
--    - Crie o banco: CREATE DATABASE crypto_monitor;
--    - Crie o usuário: CREATE USER crypto_user WITH PASSWORD 'crypto_password';
--    - Dê permissões: GRANT ALL PRIVILEGES ON DATABASE crypto_monitor TO crypto_user;
--    - Configure as variáveis de ambiente:
--      DB_USERNAME=crypto_user
--      DB_PASSWORD=crypto_password
CREATE TABLE IF NOT EXISTS trading_bot_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bot_id BIGINT REFERENCES trading_bots(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    was_simulation BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user_created ON trading_bot_audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_severity_created ON trading_bot_audit_logs(severity, created_at DESC);
CREATE INDEX idx_audit_action ON trading_bot_audit_logs(action);

COMMENT ON TABLE trading_bot_audit_logs IS
'Logs de auditoria para ações de trading bots - retenção de 90 dias';
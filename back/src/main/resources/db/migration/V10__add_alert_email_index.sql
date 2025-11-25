CREATE INDEX IF NOT EXISTS idx_alert_rules_email_active
ON alert_rules(notification_email, is_active)
WHERE is_active = true;
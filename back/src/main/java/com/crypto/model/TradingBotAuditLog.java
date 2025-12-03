package com.crypto.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trading_bot_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingBotAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id")
    private TradingBot bot;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "was_simulation")
    private Boolean wasSimulation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum AuditAction {
        BOT_CREATED,
        BOT_STARTED,
        BOT_STOPPED,
        BOT_DELETED,
        BOT_CONFIGURATION_CHANGED,
        SIMULATION_TOGGLED,           // ⚠️ Tentativa de ativar trading real
        UNAUTHORIZED_ACCESS_ATTEMPT,   // ⚠️ Acesso negado
        TRADE_EXECUTED,
        BOT_ERROR
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
}
package com.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CryptoMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoMonitorApplication.class, args);
        System.out.println("\n" +
                "🚀 Crypto Monitor iniciado com sucesso!\n" +
                "📊 API: http://localhost:8080/crypto-monitor/api/crypto\n" +
                "🤖 Trading Bots: ATIVO\n" +
                "🗃️  H2 Console: http://localhost:8080/crypto-monitor/h2-console\n" +
                "💚 Health Check: http://localhost:8080/crypto-monitor/actuator/health\n" +
                "📈 Status: http://localhost:8080/crypto-monitor/api/crypto/status\n");
    }

    /**
     * ❌ CACHE WARMUP - **DESABILITADO**
     *
     * ⚠️ MOTIVO: Causava timeout de 60 segundos na inicialização
     *
     * ✅ SOLUÇÃO: Cache é populado no primeiro request (lazy loading)
     */
    // @PostConstruct // ❌ REMOVIDO
    public void warmUpCache() {
        // Desabilitado para evitar timeout na inicialização
        // Cache será populado automaticamente no primeiro request
    }
}
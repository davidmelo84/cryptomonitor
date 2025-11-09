// CryptoMonitorApplication.java
package com.crypto;

import ch.qos.logback.classic.Logger;
import com.crypto.service.CryptoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor

public class    CryptoMonitorApplication {

    private final CryptoService cryptoService;

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

    @PostConstruct
    public void warmUpCache() {
        Logger log = null;
        log.info("🔥 Aquecendo cache na inicialização...");
        try {
            cryptoService.warmUpCache();
            log.info("✅ Cache aquecido com sucesso!");
        } catch (Exception e) {
            log.error("❌ Erro ao aquecer cache: {}", e.getMessage());
        }
    }
}
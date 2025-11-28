package com.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CryptoMonitorApplication {

    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();


        System.setProperty("spring.jmx.enabled", "false");
        System.setProperty("spring.main.lazy-initialization", "true");
        System.setProperty("server.tomcat.mbeanregistry.enabled", "false");

        SpringApplication app = new SpringApplication(CryptoMonitorApplication.class);


        app.addListeners(event -> {
            if (event instanceof org.springframework.boot.context.event.ApplicationReadyEvent) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("🚀 Aplicação iniciada em {}ms", elapsed);

                if (elapsed > 60000) {
                    log.warn("⚠️ Startup demorou mais de 60s - verifique configurações");
                }

                log.info("\n" +
                        "🚀 Crypto Monitor iniciado com sucesso!\n" +
                        "📊 API: http://localhost:8080/crypto-monitor/api/crypto\n" +
                        "🤖 Trading Bots: ATIVO\n" +
                        "🗃️  H2 Console: http://localhost:8080/crypto-monitor/h2-console\n" +
                        "💚 Health Check: http://localhost:8080/crypto-monitor/actuator/health\n" +
                        "📈 Status: http://localhost:8080/crypto-monitor/api/crypto/status\n");
            }
        });


        app.run(args);

        log.info("✅ Crypto Monitor ONLINE");
        log.info("📊 API: http://localhost:8080/crypto-monitor/api/crypto");
        log.info("💚 Health: http://localhost:8080/crypto-monitor/actuator/health");
    }


    public void warmUpCache() {
    }
}

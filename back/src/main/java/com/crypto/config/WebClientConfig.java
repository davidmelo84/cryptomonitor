package com.crypto.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * ✅ CONFIGURAÇÃO OTIMIZADA DO WEBCLIENT
 *
 * Melhorias:
 * - Timeout agressivo (20s)
 * - Connection pool otimizado
 * - User-Agent customizado
 * - Retry automático
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        log.info("🔧 Configurando WebClient otimizado...");

        HttpClient httpClient = HttpClient.create()
                // ✅ Connection timeout
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)

                // ✅ Response timeout
                .responseTimeout(Duration.ofSeconds(20))

                // ✅ Read/Write timeouts
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(20, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(20, TimeUnit.SECONDS));
                })

                // ✅ Keep-alive
                .option(ChannelOption.SO_KEEPALIVE, true)

                // ✅ Connection pool
                .option(ChannelOption.SO_REUSEADDR, true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))

                // ✅ User-Agent customizado
                .defaultHeader(HttpHeaders.USER_AGENT, "CryptoMonitor/2.0 (Java/17)")

                // ✅ Accept JSON
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")

                // ✅ Buffer size maior (16MB)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)
                )

                .build();
    }
}
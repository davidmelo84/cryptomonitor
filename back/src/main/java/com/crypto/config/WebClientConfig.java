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


@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        log.info("🔧 Configurando WebClient otimizado...");

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)

                .responseTimeout(Duration.ofSeconds(20))

                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(20, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(20, TimeUnit.SECONDS));
                })

                .option(ChannelOption.SO_KEEPALIVE, true)

                .option(ChannelOption.SO_REUSEADDR, true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))

                .defaultHeader(HttpHeaders.USER_AGENT, "CryptoMonitor/2.0 (Java/17)")

                .defaultHeader(HttpHeaders.ACCEPT, "application/json")

                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)
                )

                .build();
    }
}
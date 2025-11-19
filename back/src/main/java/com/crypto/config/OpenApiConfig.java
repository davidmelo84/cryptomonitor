package com.crypto.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/crypto-monitor}")
    private String contextPath;

    @Bean
    public OpenAPI cryptoMonitorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Crypto Monitor API")
                        .description("Sistema de Monitoramento de Criptomoedas em Tempo Real")
                        .version("2.0.1")
                        .contact(new Contact()
                                .name("Crypto Monitor Team")
                                .email("ddevnordeste@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080" + contextPath)
                                .description("Desenvolvimento"),
                        new Server()
                                .url("https://your-app.onrender.com" + contextPath)
                                .description("Produção")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Autenticação JWT. Use: Bearer {token}")));
    }
}
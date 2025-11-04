package com.crypto.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotEnvConfig {

    @PostConstruct
    public void loadEnvVariables() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            // Carrega todas as variáveis do .env para System Properties se não existirem
            dotenv.entries().forEach(entry -> {
                if (System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });

            // Log detalhado para SMTP/SendGrid
            System.out.println("✅ Arquivo .env carregado com sucesso!");
            System.out.println("📧 MAIL_USERNAME: " + dotenv.get("MAIL_USERNAME", "não configurado"));
            System.out.println("🔑 SENDGRID_API_KEY: " +
                    (dotenv.get("SENDGRID_API_KEY") != null ? "CONFIGURADO" : "NÃO CONFIGURADO"));
            System.out.println("📤 SENDGRID_FROM_EMAIL: " + dotenv.get("SENDGRID_FROM_EMAIL", "não configurado"));

        } catch (Exception e) {
            System.out.println("⚠️ Erro ao carregar arquivo .env: " + e.getMessage());
            System.out.println("💡 Usando configurações padrão do application.yml");
        }
    }
}

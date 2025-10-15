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

            // Carrega todas as variáveis do .env para as System Properties
            dotenv.entries().forEach(entry -> {
                // Só define se não existir uma variável de sistema já configurada
                if (System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });

            // Log para confirmar que carregou
            System.out.println("✅ Arquivo .env carregado com sucesso!");
            System.out.println("📧 Email configurado: " + dotenv.get("MAIL_USERNAME", "não configurado"));

        } catch (Exception e) {
            System.out.println("⚠️ Erro ao carregar arquivo .env: " + e.getMessage());
            System.out.println("💡 Usando configurações padrão do application.yml");
        }
    }
}
package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço coordenador que gerencia a comunicação entre CryptoService e AlertService
 * Resolve a dependência circular entre os serviços
 *
 * IMPORTANTE: O @Scheduled foi REMOVIDO para permitir controle manual pelo usuário
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoMonitoringService {

    private final CryptoService cryptoService;
    private final AlertService alertService;

    /**
     * ⚠️ ATENÇÃO: @Scheduled REMOVIDO
     *
     * Este método era executado automaticamente a cada 5 minutos.
     * Agora é chamado APENAS pelo MonitoringControlService quando o usuário
     * inicia o monitoramento.
     *
     * ANTES:
     * @Scheduled(fixedRate = 300000) // 5 minutos
     * public void updateAndProcessAlerts() { ... }
     *
     * AGORA: Chamado programaticamente pelo scheduler por usuário
     */
    public void updateAndProcessAlerts() {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento...");

            // 1. Buscar preços atuais
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
            log.info("📊 Obtidos preços de {} criptomoedas", currentCryptos.size());

            // 2. Salvar os dados atualizados
            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            // 3. Processar alertas com os dados atualizados
            alertService.processAlerts(currentCryptos);

            log.info("✅ Ciclo de monitoramento concluído com sucesso");

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento: {}", e.getMessage(), e);
        }
    }

    /**
     * NOVO: Atualização e processamento de alertas para um usuário específico
     *
     * Este método é chamado pelo MonitoringControlService em intervalos regulares
     * APENAS para usuários que ativaram o monitoramento.
     *
     * @param userEmail Email do usuário que receberá os alertas
     */
    public void updateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento para email: {}", userEmail);

            // 1. Buscar preços atuais da API CoinGecko
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
            log.info("📊 Obtidos preços de {} criptomoedas", currentCryptos.size());

            // 2. Salvar os dados atualizados no banco
            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            // 3. Processar alertas APENAS para este usuário específico
            alertService.processAlertsForUser(currentCryptos, userEmail);

            log.info("✅ Ciclo de monitoramento concluído para: {}", userEmail);

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento para {}: {}", userEmail, e.getMessage(), e);
        }
    }

    /**
     * Processo manual de atualização e verificação de alertas
     *
     * Mantido para compatibilidade com endpoint /api/crypto/update
     * Útil para atualização sob demanda via frontend
     */
    public void forceUpdateAndProcessAlerts() {
        try {
            log.info("🚀 Forçando atualização manual...");

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            for (CryptoCurrency crypto : currentCryptos) {
                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);
                alertService.checkAlertsForCrypto(savedCrypto);
            }

            log.info("✅ Atualização manual concluída. {} moedas processadas", currentCryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro na atualização manual: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na atualização manual", e);
        }
    }

    /**
     * NOVO: Atualização manual para um usuário específico
     *
     * Similar ao forceUpdateAndProcessAlerts, mas filtra alertas por email
     *
     * @param userEmail Email do usuário
     */
    public void forceUpdateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🚀 Forçando atualização manual para: {}", userEmail);

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            for (CryptoCurrency crypto : currentCryptos) {
                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);
                alertService.checkAlertsForCryptoAndUser(savedCrypto, userEmail);
            }

            log.info("✅ Atualização manual concluída para {}. {} moedas processadas",
                    userEmail, currentCryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro na atualização manual para {}: {}", userEmail, e.getMessage(), e);
            throw new RuntimeException("Falha na atualização manual", e);
        }
    }

    /**
     * Processa alertas para uma criptomoeda específica
     *
     * Mantido para compatibilidade com endpoint específico
     */
    public void processAlertsForCrypto(String coinId) {
        try {
            cryptoService.getCryptoByCoinId(coinId)
                    .ifPresentOrElse(
                            crypto -> {
                                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);
                                alertService.checkAlertsForCrypto(savedCrypto);
                                log.info("✅ Alertas processados para {}", coinId);
                            },
                            () -> log.warn("⚠️ Criptomoeda {} não encontrada", coinId)
                    );
        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {}: {}", coinId, e.getMessage());
        }
    }

    /**
     * NOVO: Processa alertas para uma criptomoeda específica E usuário específico
     *
     * @param coinId ID da criptomoeda (ex: "bitcoin")
     * @param userEmail Email do usuário
     */
    public void processAlertsForCryptoAndUser(String coinId, String userEmail) {
        try {
            log.info("🔍 Processando alertas de {} para {}", coinId, userEmail);

            cryptoService.getCryptoByCoinId(coinId)
                    .ifPresentOrElse(
                            crypto -> {
                                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);
                                alertService.checkAlertsForCryptoAndUser(savedCrypto, userEmail);
                                log.info("✅ Alertas processados para {} (usuário: {})", coinId, userEmail);
                            },
                            () -> log.warn("⚠️ Criptomoeda {} não encontrada", coinId)
                    );
        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {} (usuário: {}): {}",
                    coinId, userEmail, e.getMessage());
        }
    }

    /**
     * NOVO: Retorna estatísticas do monitoramento
     *
     * Útil para dashboard e debugging
     */
    public MonitoringStats getMonitoringStats() {
        try {
            List<CryptoCurrency> savedCryptos = cryptoService.getAllSavedCryptos();
            long totalAlerts = alertService.getActiveAlertRules().size();

            return MonitoringStats.builder()
                    .totalCryptocurrencies(savedCryptos.size())
                    .totalActiveAlerts(totalAlerts)
                    .lastUpdate(savedCryptos.isEmpty() ? null :
                            savedCryptos.get(0).getLastUpdated())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas: {}", e.getMessage());
            return MonitoringStats.builder()
                    .totalCryptocurrencies(0)
                    .totalActiveAlerts(0)
                    .build();
        }
    }

    /**
     * Classe interna para estatísticas
     */
    @lombok.Builder
    @lombok.Data
    public static class MonitoringStats {
        private int totalCryptocurrencies;
        private long totalActiveAlerts;
        private java.time.LocalDateTime lastUpdate;
    }
}

/*
 * ============================================
 * CHANGELOG - O QUE MUDOU:
 * ============================================
 *
 * 1. REMOVIDO: @Scheduled(fixedRate = 300000)
 *    - Não executa mais automaticamente
 *    - Agora controlado pelo MonitoringControlService
 *
 * 2. ADICIONADO: updateAndProcessAlertsForUser(String userEmail)
 *    - Método chamado pelo scheduler por usuário
 *    - Processa alertas APENAS para o email especificado
 *
 * 3. ADICIONADO: forceUpdateAndProcessAlertsForUser(String userEmail)
 *    - Atualização manual filtrada por usuário
 *
 * 4. ADICIONADO: processAlertsForCryptoAndUser(String coinId, String userEmail)
 *    - Processa alertas de uma crypto específica para um usuário
 *
 * 5. ADICIONADO: getMonitoringStats()
 *    - Retorna estatísticas do sistema
 *
 * 6. MANTIDO: Métodos originais para compatibilidade
 *    - updateAndProcessAlerts()
 *    - forceUpdateAndProcessAlerts()
 *    - processAlertsForCrypto(String coinId)
 *
 * ============================================
 * COMO FUNCIONA AGORA:
 * ============================================
 *
 * ANTES:
 * - Spring Boot inicia → @Scheduled executa automaticamente
 * - Processa alertas de TODOS os usuários a cada 5 min
 *
 * AGORA:
 * - Spring Boot inicia → Nenhum monitoramento ativo
 * - Usuário clica "Iniciar Monitoramento" no frontend
 * - Frontend chama: POST /api/monitoring/start { email }
 * - MonitoringControlService cria scheduler EXCLUSIVO
 * - Scheduler chama: updateAndProcessAlertsForUser(email)
 * - Processa alertas APENAS deste usuário
 *
 * VANTAGENS:
 * ✅ Frontend carrega sem conflitos
 * ✅ Cada usuário controla seu monitoramento
 * ✅ Multi-usuário suportado
 * ✅ Economia de recursos (só monitora quem ativou)
 */
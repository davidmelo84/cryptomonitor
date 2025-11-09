package com.crypto.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ✅ FILA CENTRALIZADA PARA REQUESTS DO COINGECKO
 *
 * SOLUÇÃO PARA MÚLTIPLOS USUÁRIOS:
 * - Todos os requests passam por esta fila
 * - Rate limit global: 1 request a cada 2 segundos
 * - Máximo 30 requests por minuto
 * - Timeout de 30 segundos por request
 *
 * EXEMPLO:
 * 10 usuários iniciam monitoramento simultaneamente
 * → 10 requests enfileirados
 * → Processados 1 por vez (2s intervalo)
 * → Total: 20 segundos (vs rate limit instantâneo)
 */
@Slf4j
@Service
public class CoinGeckoRequestQueue {

    // ✅ Fila thread-safe com prioridade
    private final PriorityBlockingQueue<QueuedRequest> requestQueue =
            new PriorityBlockingQueue<>(100);

    // ✅ Executor para processar fila
    private final ExecutorService queueProcessor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "CoinGecko-Queue-Processor");
                t.setDaemon(true);
                return t;
            });

    // ✅ Rate limit tracking
    private final ConcurrentLinkedQueue<Instant> recentRequests =
            new ConcurrentLinkedQueue<>();

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger queuedRequests = new AtomicInteger(0);

    private static final long MIN_INTERVAL_MS = 2000; // 2 segundos
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long REQUEST_TIMEOUT_MS = 30000; // 30 segundos

    private volatile Instant lastRequestTime = Instant.now();

    public CoinGeckoRequestQueue() {
        // ✅ Iniciar processador da fila
        queueProcessor.submit(this::processQueue);

        log.info("✅ CoinGecko Request Queue inicializada");
        log.info("   Rate Limit: {} req/min, {} ms entre requests",
                MAX_REQUESTS_PER_MINUTE, MIN_INTERVAL_MS);
    }

    /**
     * ✅ ENFILEIRAR REQUEST
     *
     * @param supplier Função que faz o request
     * @param priority ALTA (0) = schedulers, NORMAL (1) = usuários, BAIXA (2) = background
     * @return CompletableFuture com resultado
     */
    public <T> CompletableFuture<T> enqueue(
            Callable<T> supplier,
            RequestPriority priority
    ) {
        CompletableFuture<T> future = new CompletableFuture<>();

        QueuedRequest request = new QueuedRequest(supplier, future, priority);

        queuedRequests.incrementAndGet();
        requestQueue.offer(request);

        log.debug("📥 Request enfileirado (Prioridade: {}, Fila: {})",
                priority, requestQueue.size());

        return future;
    }

    /**
     * ✅ PROCESSAR FILA (loop infinito)
     */
    private void processQueue() {
        log.info("🔄 Queue processor iniciado");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                // ✅ Aguardar próximo request (blocking)
                QueuedRequest request = requestQueue.poll(1, TimeUnit.SECONDS);

                if (request == null) {
                    continue; // Sem requests na fila
                }

                // ✅ Verificar timeout
                if (request.isExpired(REQUEST_TIMEOUT_MS)) {
                    log.warn("⏰ Request expirado após {}ms na fila",
                            REQUEST_TIMEOUT_MS);
                    request.future.completeExceptionally(
                            new TimeoutException("Request timeout na fila")
                    );
                    continue;
                }

                // ✅ Aguardar rate limit
                waitForRateLimit();

                // ✅ Executar request
                executeRequest(request);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ Queue processor interrompido");
                break;

            } catch (Exception e) {
                log.error("❌ Erro no queue processor: {}", e.getMessage(), e);
            }
        }

        log.info("🛑 Queue processor finalizado");
    }

    /**
     * ✅ EXECUTAR REQUEST
     */
    @SuppressWarnings("unchecked")
    private <T> void executeRequest(QueuedRequest request) {
        try {
            log.debug("🚀 Executando request (Prioridade: {})", request.priority);

            // ✅ Executar
            T result = (T) request.callable.call();

            // ✅ Completar future
            ((CompletableFuture<T>) request.future).complete(result);

            // ✅ Registrar sucesso
            totalRequests.incrementAndGet();
            queuedRequests.decrementAndGet();
            lastRequestTime = Instant.now();

            log.debug("✅ Request executado com sucesso");

        } catch (Exception e) {
            log.error("❌ Erro ao executar request: {}", e.getMessage());
            request.future.completeExceptionally(e);
            queuedRequests.decrementAndGet();
        }
    }

    /**
     * ✅ AGUARDAR RATE LIMIT
     */
    private void waitForRateLimit() throws InterruptedException {
        // 1️⃣ Limpar requests antigos (> 1 minuto)
        cleanOldRequests();

        // 2️⃣ Verificar limite por minuto
        if (recentRequests.size() >= MAX_REQUESTS_PER_MINUTE) {
            Instant oldest = recentRequests.peek();
            if (oldest != null) {
                long waitMs = Duration.between(oldest, Instant.now()).toMillis();
                waitMs = 60000 - waitMs; // Tempo até completar 1 minuto

                if (waitMs > 0) {
                    log.warn("⏸️ Rate limit atingido! Aguardando {}ms...", waitMs);
                    Thread.sleep(waitMs);
                }
            }
        }

        // 3️⃣ Verificar intervalo mínimo
        long elapsed = Duration.between(lastRequestTime, Instant.now()).toMillis();
        if (elapsed < MIN_INTERVAL_MS) {
            long waitMs = MIN_INTERVAL_MS - elapsed;
            log.debug("⏳ Aguardando {}ms (intervalo mínimo)...", waitMs);
            Thread.sleep(waitMs);
        }

        // 4️⃣ Registrar request
        recentRequests.offer(Instant.now());
    }

    /**
     * ✅ LIMPAR REQUESTS ANTIGOS
     */
    private void cleanOldRequests() {
        Instant oneMinuteAgo = Instant.now().minus(Duration.ofMinutes(1));
        recentRequests.removeIf(instant -> instant.isBefore(oneMinuteAgo));
    }

    /**
     * ✅ ESTATÍSTICAS DA FILA
     */
    public QueueStats getStats() {
        return new QueueStats(
                requestQueue.size(),
                queuedRequests.get(),
                totalRequests.get(),
                recentRequests.size(),
                lastRequestTime
        );
    }

    // =========================================
    // CLASSES AUXILIARES
    // =========================================

    /**
     * Request enfileirado
     */
    private static class QueuedRequest implements Comparable<QueuedRequest> {
        final Callable<?> callable;
        final CompletableFuture<?> future;
        final RequestPriority priority;
        final Instant enqueuedAt;

        QueuedRequest(Callable<?> callable, CompletableFuture<?> future,
                      RequestPriority priority) {
            this.callable = callable;
            this.future = future;
            this.priority = priority;
            this.enqueuedAt = Instant.now();
        }

        boolean isExpired(long timeoutMs) {
            return Duration.between(enqueuedAt, Instant.now()).toMillis() > timeoutMs;
        }

        @Override
        public int compareTo(QueuedRequest other) {
            // Menor valor = maior prioridade
            return Integer.compare(this.priority.value, other.priority.value);
        }
    }

    /**
     * Prioridade do request
     */
    public enum RequestPriority {
        HIGH(0),    // Schedulers críticos
        NORMAL(1),  // Requests de usuários
        LOW(2);     // Background tasks

        final int value;

        RequestPriority(int value) {
            this.value = value;
        }
    }

    /**
     * Estatísticas
     */
    public record QueueStats(
            int queueSize,
            int queuedRequests,
            int totalProcessed,
            int requestsLastMinute,
            Instant lastRequestTime
    ) {}
}
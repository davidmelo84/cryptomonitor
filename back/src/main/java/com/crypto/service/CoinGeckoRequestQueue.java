package com.crypto.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ✅ FILA CENTRALIZADA PARA REQUESTS DO COINGECKO
 *
 * 🔁 Agora com deduplicação de requisições idênticas:
 * - Requests com a mesma chave (requestKey) compartilham o mesmo Future
 * - Evita chamadas repetidas para o mesmo endpoint em paralelo
 *
 * SOLUÇÃO PARA MÚLTIPLOS USUÁRIOS:
 * - Todos os requests passam por esta fila
 * - Rate limit global: 1 request a cada 2 segundos
 * - Máximo 30 requests por minuto
 * - Timeout de 30 segundos por request
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

    // ✅ Map para deduplicação
    private final Map<String, CompletableFuture<?>> pendingRequests =
            new ConcurrentHashMap<>(); // ✅ ADICIONADO

    // ✅ Rate limit tracking
    private final ConcurrentLinkedQueue<Instant> recentRequests =
            new ConcurrentLinkedQueue<>();

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger queuedRequests = new AtomicInteger(0);

    private static final long MIN_INTERVAL_MS = 5000; // 2 segundos
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long REQUEST_TIMEOUT_MS = 30000; // 30 segundos

    private volatile Instant lastRequestTime = Instant.now();

    public CoinGeckoRequestQueue() {
        queueProcessor.submit(this::processQueue);
        log.info("✅ CoinGecko Request Queue inicializada");
        log.info("   Rate Limit: {} req/min, {} ms entre requests",
                MAX_REQUESTS_PER_MINUTE, MIN_INTERVAL_MS);
    }

    /**
     * ✅ ENFILEIRAR REQUEST (com deduplicação)
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> enqueue(
            Callable<T> supplier,
            RequestPriority priority
    ) {
        // ✅ Gerar chave única baseada no callable
        String requestKey = generateRequestKey(supplier);

        // ✅ Reutilizar request pendente
        CompletableFuture<?> existing = pendingRequests.get(requestKey);
        if (existing != null && !existing.isDone()) {
            log.debug("♻️ Reusando request existente: {}", requestKey);
            return (CompletableFuture<T>) existing;
        }

        // ✅ Criar novo future e registrar no mapa
        CompletableFuture<T> future = new CompletableFuture<>();
        pendingRequests.put(requestKey, future);

        // ✅ Criar e enfileirar
        QueuedRequest request = new QueuedRequest(supplier, future, priority, requestKey);
        queuedRequests.incrementAndGet();
        requestQueue.offer(request);

        // ✅ Remover do mapa ao concluir
        future.whenComplete((result, error) -> pendingRequests.remove(requestKey));

        log.debug("📥 Request enfileirado (Prioridade: {}, Fila: {})",
                priority, requestQueue.size());

        return future;
    }

    // ✅ Gera uma chave única para deduplicação
    private String generateRequestKey(Callable<?> supplier) {
        return supplier.getClass().getName() + "@" + System.identityHashCode(supplier);
    }

    /**
     * ✅ PROCESSAR FILA
     */
    private void processQueue() {
        log.info("🔄 Queue processor iniciado");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                QueuedRequest request = requestQueue.poll(1, TimeUnit.SECONDS);

                if (request == null) continue;

                if (request.isExpired(REQUEST_TIMEOUT_MS)) {
                    log.warn("⏰ Request expirado após {}ms na fila", REQUEST_TIMEOUT_MS);
                    request.future.completeExceptionally(
                            new TimeoutException("Request timeout na fila"));
                    continue;
                }

                waitForRateLimit();
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

            T result = (T) request.callable.call();
            ((CompletableFuture<T>) request.future).complete(result);

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
        cleanOldRequests();

        if (recentRequests.size() >= MAX_REQUESTS_PER_MINUTE) {
            Instant oldest = recentRequests.peek();
            if (oldest != null) {
                long waitMs = Duration.between(oldest, Instant.now()).toMillis();
                waitMs = 60000 - waitMs;
                if (waitMs > 0) {
                    log.warn("⏸️ Rate limit atingido! Aguardando {}ms...", waitMs);
                    Thread.sleep(waitMs);
                }
            }
        }

        long elapsed = Duration.between(lastRequestTime, Instant.now()).toMillis();
        if (elapsed < MIN_INTERVAL_MS) {
            long waitMs = MIN_INTERVAL_MS - elapsed;
            log.debug("⏳ Aguardando {}ms (intervalo mínimo)...", waitMs);
            Thread.sleep(waitMs);
        }

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
        final String requestKey;  // ✅ ADICIONADO

        QueuedRequest(Callable<?> callable, CompletableFuture<?> future,
                      RequestPriority priority, String requestKey) { // ✅ ADICIONADO
            this.callable = callable;
            this.future = future;
            this.priority = priority;
            this.enqueuedAt = Instant.now();
            this.requestKey = requestKey; // ✅ ADICIONADO
        }

        boolean isExpired(long timeoutMs) {
            return Duration.between(enqueuedAt, Instant.now()).toMillis() > timeoutMs;
        }

        @Override
        public int compareTo(QueuedRequest other) {
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

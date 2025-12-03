package com.crypto.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class CoinGeckoRequestQueue {

    // ======= CONFIGS VINDAS DO application.yml ========
    @Value("${coingecko.api.rate-limit.min-request-interval-ms:500}")
    private long MIN_INTERVAL_MS;

    @Value("${coingecko.api.rate-limit.requests-per-minute:30}")
    private int MAX_REQUESTS_PER_MINUTE;

    @Value("${coingecko.api.rate-limit.request-timeout-ms:30000}")
    private long REQUEST_TIMEOUT_MS;
    // ===================================================

    private final PriorityBlockingQueue<QueuedRequest> requestQueue =
            new PriorityBlockingQueue<>(100);

    private final ExecutorService queueProcessor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "CoinGecko-Queue-Processor");
                t.setDaemon(true);
                return t;
            });

    private final Map<String, CompletableFuture<?>> pendingRequests =
            new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Instant> recentRequests =
            new ConcurrentLinkedQueue<>();

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger queuedRequests = new AtomicInteger(0);

    private volatile Instant lastRequestTime = Instant.now();

    public CoinGeckoRequestQueue() {
        queueProcessor.submit(this::processQueue);
        log.info("✅ CoinGecko Request Queue inicializada");

        log.info("   Rate Limit configurado: {} req/min, intervalo mínimo {} ms",
                MAX_REQUESTS_PER_MINUTE,
                MIN_INTERVAL_MS);
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> enqueue(
            Callable<T> supplier,
            RequestPriority priority
    ) {
        String requestKey = generateRequestKey(supplier);

        CompletableFuture<?> existing = pendingRequests.get(requestKey);
        if (existing != null && !existing.isDone()) {
            log.debug("♻️ Reusando request existente: {}", requestKey);
            return (CompletableFuture<T>) existing;
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        pendingRequests.put(requestKey, future);

        QueuedRequest request = new QueuedRequest(supplier, future, priority, requestKey);
        queuedRequests.incrementAndGet();
        requestQueue.offer(request);

        future.whenComplete((result, error) -> pendingRequests.remove(requestKey));

        log.debug("📥 Request enfileirado (Prioridade: {}, Fila: {})",
                priority, requestQueue.size());

        return future;
    }

    private String generateRequestKey(Callable<?> supplier) {
        return supplier.getClass().getName() + "@" + System.identityHashCode(supplier);
    }

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
            String message = e.getMessage() != null ? e.getMessage() : "";

            if (message.contains("429") || message.contains("Too Many Requests")) {
                log.warn("⚠️ Erro HTTP 429 detectado! Entrando em cooldown por 60 segundos...");
                try {
                    Thread.sleep(60000); // pausa 1 minuto
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } else {
                log.error("❌ Erro ao executar request: {}", message);
            }

            request.future.completeExceptionally(e);
            queuedRequests.decrementAndGet();
        }
    }

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
            log.info("⏳ Respeitando intervalo mínimo entre requisições: {}ms", waitMs);
            Thread.sleep(waitMs);
        }

        recentRequests.offer(Instant.now());
    }

    private void cleanOldRequests() {
        Instant oneMinuteAgo = Instant.now().minus(Duration.ofMinutes(1));
        recentRequests.removeIf(instant -> instant.isBefore(oneMinuteAgo));
    }

    public QueueStats getStats() {
        return new QueueStats(
                requestQueue.size(),
                queuedRequests.get(),
                totalRequests.get(),
                recentRequests.size(),
                lastRequestTime
        );
    }

    private static class QueuedRequest implements Comparable<QueuedRequest> {
        final Callable<?> callable;
        final CompletableFuture<?> future;
        final RequestPriority priority;
        final Instant enqueuedAt;
        final String requestKey;

        QueuedRequest(Callable<?> callable, CompletableFuture<?> future,
                      RequestPriority priority, String requestKey) {
            this.callable = callable;
            this.future = future;
            this.priority = priority;
            this.enqueuedAt = Instant.now();
            this.requestKey = requestKey;
        }

        boolean isExpired(long timeoutMs) {
            return Duration.between(enqueuedAt, Instant.now()).toMillis() > timeoutMs;
        }

        @Override
        public int compareTo(QueuedRequest other) {
            return Integer.compare(this.priority.value, other.priority.value);
        }
    }

    public enum RequestPriority {
        HIGH(0),
        NORMAL(1),
        LOW(2);

        final int value;

        RequestPriority(int value) {
            this.value = value;
        }
    }

    public record QueueStats(
            int queueSize,
            int queuedRequests,
            int totalProcessed,
            int requestsLastMinute,
            Instant lastRequestTime
    ) {}
}

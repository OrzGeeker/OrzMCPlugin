package com.jokerhub.paper.plugin.orzmc.infra.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class AsyncHttp {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 500;
    private static final ConcurrentMap<Duration, HttpClient> CLIENTS = new ConcurrentHashMap<>();

    private static HttpClient client(Duration connectTimeout) {
        Duration timeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        return CLIENTS.computeIfAbsent(
                timeout, value -> HttpClient.newBuilder().connectTimeout(value).build());
    }

    private static CompletableFuture<HttpResponse<String>> sendWithRetry(
            HttpClient c, HttpRequest request, int retries) {
        return sendWithRetry(c, request, Math.max(0, retries), 0);
    }

    private static CompletableFuture<HttpResponse<String>> sendWithRetry(
            HttpClient c, HttpRequest request, int retriesRemaining, int attempt) {
        return c.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((resp, ex) -> {
                    boolean retryableStatus = resp != null && (resp.statusCode() == 429 || resp.statusCode() >= 500);
                    if (ex == null && !retryableStatus) {
                        return CompletableFuture.completedFuture(resp);
                    }
                    if (retriesRemaining <= 0) {
                        return ex == null
                                ? CompletableFuture.completedFuture(resp)
                                : CompletableFuture.<HttpResponse<String>>failedFuture(ex);
                    }
                    long delay = BASE_BACKOFF_MS * (1L << Math.min(attempt, 10));
                    java.util.concurrent.Executor delayed =
                            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS);
                    return CompletableFuture.supplyAsync(() -> null, delayed)
                            .thenCompose(v -> sendWithRetry(c, request, retriesRemaining - 1, attempt + 1));
                })
                .thenCompose(f -> f);
    }

    public static CompletableFuture<HttpResponse<String>> get(
            String url,
            Map<String, String> headers,
            Duration connectTimeout,
            Duration requestTimeout,
            Integer maxRetries) {
        HttpClient c = client(connectTimeout);
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout == null ? DEFAULT_REQUEST_TIMEOUT : requestTimeout);
        if (headers != null) headers.forEach(b::setHeader);
        HttpRequest req = b.GET().build();
        return sendWithRetry(c, req, maxRetries == null ? DEFAULT_MAX_RETRIES : maxRetries);
    }

    public static CompletableFuture<HttpResponse<String>> postJson(
            String url,
            String json,
            Map<String, String> headers,
            Duration connectTimeout,
            Duration requestTimeout,
            Integer maxRetries) {
        HttpClient c = client(connectTimeout);
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout == null ? DEFAULT_REQUEST_TIMEOUT : requestTimeout)
                .header("Content-Type", "application/json");
        if (headers != null) headers.forEach(b::setHeader);
        HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json))
                .build();
        return sendWithRetry(c, req, maxRetries == null ? DEFAULT_MAX_RETRIES : maxRetries);
    }
}

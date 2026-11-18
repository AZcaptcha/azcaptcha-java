package io.azcaptcha;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** CompletableFuture facade; each call runs the sync client on the executor. */
public final class AzcaptchaAsyncClient {
    private final AzcaptchaClient client;
    private final Executor executor;

    AzcaptchaAsyncClient(AzcaptchaClient client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    public CompletableFuture<String> solve(String method, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> client.solve(method, params), executor);
    }

    public CompletableFuture<String> solve(String method, Map<String, Object> params, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> client.solve(method, params, timeout), executor);
    }

    public CompletableFuture<String> solveImage(byte[] image, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> client.solveImage(image, params), executor);
    }

    public CompletableFuture<String> solveImageFile(Path path, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> client.solveImageFile(path, params), executor);
    }

    public CompletableFuture<String> solveRecaptchaV2(String sitekey, String pageurl) {
        return CompletableFuture.supplyAsync(() -> client.solveRecaptchaV2(sitekey, pageurl), executor);
    }

    public CompletableFuture<String> solveRecaptchaV3(String sitekey, String pageurl, String action) {
        return CompletableFuture.supplyAsync(() -> client.solveRecaptchaV3(sitekey, pageurl, action), executor);
    }

    public CompletableFuture<String> solveHCaptcha(String sitekey, String pageurl) {
        return CompletableFuture.supplyAsync(() -> client.solveHCaptcha(sitekey, pageurl), executor);
    }

    public CompletableFuture<String> solveTurnstile(String sitekey, String pageurl) {
        return CompletableFuture.supplyAsync(() -> client.solveTurnstile(sitekey, pageurl), executor);
    }

    public CompletableFuture<Double> balance() {
        return CompletableFuture.supplyAsync(client::balance, executor);
    }

    public CompletableFuture<Boolean> reportBad(String taskId) {
        return CompletableFuture.supplyAsync(() -> client.reportBad(taskId), executor);
    }

    public CompletableFuture<Integer> createTask(String type, Map<String, Object> task) {
        return CompletableFuture.supplyAsync(() -> client.createTask(type, task), executor);
    }

    public CompletableFuture<Map<String, Object>> getTaskResult(int taskId) {
        return CompletableFuture.supplyAsync(() -> client.getTaskResult(taskId), executor);
    }

    public CompletableFuture<Map<String, Object>> waitTaskResult(int taskId, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> client.waitTaskResult(taskId, timeout), executor);
    }
}

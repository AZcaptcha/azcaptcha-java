package io.azcaptcha;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Primary client: legacy + modern APIs, typed helpers and async facade. */
public class AzcaptchaClient extends LegacyClient {
    public static final int SOFT_ID = 0;
    public static final String NOT_READY = "CAPCHA_NOT_READY";

    public AzcaptchaClient(String apiKey) {
        this(apiKey, new AzcaptchaConfig());
    }

    public AzcaptchaClient(String apiKey, AzcaptchaConfig config) {
        super(apiKey, config);
        this.executor = config.executor;
    }

    private final java.util.concurrent.Executor executor;

    // ── modern API ────────────────────────────────────────────────────
    public int createTask(String type, Map<String, Object> task) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("type", type);
        if (task != null) {
            task.forEach((key, value) -> {
                if (value != null) {
                    merged.put(key, value);
                }
            });
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clientKey", apiKey);
        payload.put("task", merged);
        if (softId > 0) {
            payload.put("softId", softId);
        }
        Map<String, Object> body = http.postJson("/createTask", payload);
        raiseModern(body);
        Object taskId = body.get("taskId");
        return taskId instanceof Number ? ((Number) taskId).intValue() : 0;
    }

    public Map<String, Object> getTaskResult(int taskId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clientKey", apiKey);
        payload.put("taskId", taskId);
        Map<String, Object> body = http.postJson("/getTaskResult", payload);
        raiseModern(body);
        return body;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> waitTaskResult(int taskId, Duration timeout) {
        long limit = timeout == null ? 300_000L : timeout.toMillis();
        long deadline = System.nanoTime() + limit * 1_000_000L;
        long delay = 1_000L;
        while (true) {
            Map<String, Object> body = getTaskResult(taskId);
            if ("ready".equals(body.get("status"))) {
                Object solution = body.get("solution");
                return solution instanceof Map ? (Map<String, Object>) solution : Map.of();
            }
            if (System.nanoTime() + delay * 1_000_000L >= deadline) {
                throw ErrorCodes.error("ERROR_TIMEOUT", "modern task " + taskId + " not ready");
            }
            pause(delay);
            delay = 5_000L;
        }
    }

    // ── helpers ───────────────────────────────────────────────────────
    public String solve(String method, Map<String, Object> params) {
        return solve(method, params, Params.timeoutFor(method));
    }

    public String solve(String method, Map<String, Object> params, Duration timeout) {
        String taskId = submit(method, params);
        return waitResult(taskId, timeout);
    }

    public String solveImage(byte[] image, Map<String, Object> params) {
        return solve("base64", Params.merge(params == null ? Map.of() : params, "body", Params.imageBody(image)));
    }

    public String solveImageFile(Path path, Map<String, Object> params) {
        return solve("base64", Params.merge(params == null ? Map.of() : params, "body", Params.imageFile(path)));
    }

    public String solveImageBase64(String image, Map<String, Object> params) {
        return solve("base64", Params.merge(params == null ? Map.of() : params, "body", Params.imageBase64(image)));
    }

    public String solveRecaptchaV2(String sitekey, String pageurl) {
        return solveRecaptchaV2(sitekey, pageurl, false, false, null);
    }

    public String solveRecaptchaV2(String sitekey, String pageurl, boolean invisible,
                                   boolean enterprise, Map<String, Object> params) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("googlekey", sitekey);
        values.put("pageurl", pageurl);
        if (invisible) {
            values.put("invisible", 1);
        }
        if (enterprise) {
            values.put("enterprise", 1);
        }
        if (params != null) {
            values.putAll(params);
        }
        return solve("userrecaptcha", values);
    }

    public String solveRecaptchaV3(String sitekey, String pageurl, String action) {
        return solveRecaptchaV3(sitekey, pageurl, action, 0.6, null);
    }

    public String solveRecaptchaV3(String sitekey, String pageurl, String action,
                                   double minScore, Map<String, Object> params) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("googlekey", sitekey);
        values.put("pageurl", pageurl);
        values.put("version", "v3");
        values.put("action", action);
        values.put("min_score", minScore);
        if (params != null) {
            values.putAll(params);
        }
        return solve("userrecaptcha", values);
    }

    public String solveHCaptcha(String sitekey, String pageurl) {
        return solve("hcaptcha", Params.merge(Map.of(), "sitekey", sitekey, "pageurl", pageurl));
    }

    public String solveTurnstile(String sitekey, String pageurl) {
        return solve("turnstile", Params.merge(Map.of(), "sitekey", sitekey, "pageurl", pageurl));
    }

    public String solveFunCaptcha(String publickey, String pageurl) {
        return solve("funcaptcha", Params.merge(Map.of(), "publickey", publickey, "pageurl", pageurl));
    }

    public String solveGeeTest(String gt, String challenge, String pageurl) {
        return solve("geetest", Params.merge(Map.of(), "gt", gt, "challenge", challenge, "pageurl", pageurl));
    }

    public AzcaptchaAsyncClient async() {
        return new AzcaptchaAsyncClient(this, executor);
    }
}

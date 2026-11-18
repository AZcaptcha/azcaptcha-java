package io.azcaptcha;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Legacy in.php/res.php surface plus contract polling. */
public class LegacyClient extends BaseClient {
    protected LegacyClient(String apiKey, AzcaptchaConfig config) {
        super(apiKey, config);
    }

    public String submit(String method, Map<String, Object> params) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("key", apiKey);
        form.put("method", method);
        form.put("json", "1");
        if (softId > 0) {
            form.put("soft_id", String.valueOf(softId));
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        if (params != null) {
            extra.putAll(params);
        }
        Object proxy = extra.remove("proxy");
        if (proxy instanceof String && !((String) proxy).isEmpty()) {
            String[] normalized = Params.normalizeProxy((String) proxy);
            form.put("proxy", normalized[0]);
            form.put("proxytype", normalized[1]);
        }
        Object proxyType = extra.remove("proxytype");
        if (proxyType != null) {
            form.put("proxytype", String.valueOf(proxyType).toUpperCase());
        }
        extra.forEach((key, value) -> {
            if (value != null) {
                form.put(key, String.valueOf(value));
            }
        });
        return unwrap(http.postForm("/in.php", form));
    }

    public String result(String taskId) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("key", apiKey);
        query.put("action", "get");
        query.put("id", taskId);
        query.put("json", "1");
        Map<String, Object> body = http.get("/res.php", query);
        if ("CAPCHA_NOT_READY".equals(body.get("request"))) {
            throw ErrorCodes.error("CAPCHA_NOT_READY");
        }
        return unwrap(body);
    }

    public String waitResult(String taskId, Duration timeout) {
        long limit = timeout == null ? 300_000L : timeout.toMillis();
        long deadline = System.nanoTime() + limit * 1_000_000L;
        long delay = 1_000L;
        long backoff = 10_000L;
        while (true) {
            long pause;
            try {
                return result(taskId);
            } catch (AzcaptchaException error) {
                if ("CAPCHA_NOT_READY".equals(error.code())) {
                    pause = delay;
                    delay = 5_000L;
                } else if (error.category() == AzcaptchaException.Category.CAPACITY
                        || (error.category() == AzcaptchaException.Category.LIMIT && error.retryable())) {
                    pause = backoff;
                    backoff = Math.min((long) (backoff * 1.5), 60_000L);
                } else {
                    throw error;
                }
            }
            if (System.nanoTime() + pause * 1_000_000L >= deadline) {
                throw ErrorCodes.error("ERROR_TIMEOUT", "task " + taskId + " not ready");
            }
            pause(pause);
        }
    }

    public double balance() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("key", apiKey);
        query.put("action", "getbalance");
        query.put("json", "1");
        return Double.parseDouble(unwrap(http.get("/res.php", query)));
    }

    public boolean reportBad(String taskId) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("key", apiKey);
        query.put("action", "reportbad");
        query.put("id", taskId);
        query.put("json", "1");
        return unwrap(http.get("/res.php", query)).toUpperCase().startsWith("OK");
    }

    public static LegacyClient create(String apiKey, AzcaptchaConfig config) {
        return new LegacyClient(apiKey, config);
    }
}

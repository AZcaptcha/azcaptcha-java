package io.azcaptcha;

import java.util.Map;

/** Shared state and envelope handling for the sync and async surfaces. */
abstract class BaseClient {
    protected final String apiKey;
    protected final int softId;
    protected final Transport http;
    protected final Sleeper sleeper;

    BaseClient(String apiKey, AzcaptchaConfig config) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw ErrorCodes.error("ERROR_KEY_DOES_NOT_EXIST", "apiKey is required");
        }
        this.apiKey = apiKey;
        this.softId = config.softId;
        this.http = config.transport != null
                ? config.transport
                : new HttpTransport(config.host, config.httpTimeout);
        this.sleeper = config.sleeper;
    }

    protected String unwrap(Map<String, Object> body) {
        Object request = body.get("request");
        if (request instanceof String && ((String) request).startsWith("ERROR_")) {
            throw ErrorCodes.error((String) request);
        }
        Object status = body.get("status");
        if (!(status instanceof Number) || ((Number) status).intValue() != 1) {
            throw ErrorCodes.error(request instanceof String ? (String) request : "ERROR_REQUEST_FAILED");
        }
        return request == null ? "" : String.valueOf(request);
    }

    protected static void raiseModern(Map<String, Object> body) {
        Object errorId = body.get("errorId");
        int id = errorId instanceof Number ? ((Number) errorId).intValue() : 0;
        if (id == 0) {
            return;
        }
        Object code = body.get("errorCode");
        Object description = body.get("errorDescription");
        throw ErrorCodes.error(
                code instanceof String ? (String) code : "ERROR_UNKNOWN",
                description instanceof String ? (String) description : null);
    }

    protected void pause(long millis) {
        try {
            sleeper.sleep(java.time.Duration.ofMillis(millis));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AzcaptchaException("ERROR_NETWORK", AzcaptchaException.Category.NETWORK,
                    true, "interrupted");
        }
    }
}

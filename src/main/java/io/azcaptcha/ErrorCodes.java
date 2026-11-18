package io.azcaptcha;

import java.util.LinkedHashMap;
import java.util.Map;

import io.azcaptcha.AzcaptchaException.Category;

/** Maps every API code from spec/errors.json to a category and retry flag. */
public final class ErrorCodes {
    public static final class Meta {
        public final Category category;
        public final boolean retryable;

        Meta(Category category, boolean retryable) {
            this.category = category;
            this.retryable = retryable;
        }
    }

    private static final Map<String, Meta> TABLE = build();

    private ErrorCodes() {
    }

    public static AzcaptchaException error(String code) {
        return error(code, null);
    }

    public static AzcaptchaException error(String code, String message) {
        Meta meta = TABLE.getOrDefault(code, new Meta(Category.UNKNOWN, false));
        return new AzcaptchaException(code, meta.category, meta.retryable, message);
    }

    /** code -> retryable, for tests and callers. */
    public static Map<String, Boolean> taxonomy() {
        Map<String, Boolean> out = new LinkedHashMap<>();
        TABLE.forEach((code, meta) -> out.put(code, meta.retryable));
        return out;
    }

    private static Map<String, Meta> build() {
        Map<String, Meta> table = new LinkedHashMap<>();
        table.put("CAPCHA_NOT_READY", new Meta(Category.PROCESSING, true));
        auth(table, "ERROR_KEY_DOES_NOT_EXIST", "ERROR_ACCOUNT_SUSPENDED");
        table.put("ERROR_ZERO_BALANCE", new Meta(Category.BALANCE, false));
        request(table, "ERROR_BAD_PARAMETERS", "ERROR_WRONG_ID_FORMAT", "ERROR_NO_SUCH_CAPCHA_ID",
                "ERROR_TASK_NOT_FOUND", "ERROR_TASK_NOT_SUPPORTED", "ERROR_WRONG_GOOGLEKEY",
                "ERROR_RECAPTCHA_INVALID_SITEKEY", "ERROR_WRONG_PAGEURL", "ERROR_IMAGE_NOT_FOUND",
                "ERROR_IMAGE_TYPE_NOT_SUPPORTED", "ERROR_ZERO_CAPTCHA_FILESIZE",
                "ERROR_TOO_BIG_CAPTCHA_FILESIZE");
        table.put("ERROR_DAILY_LIMIT_EXCEEDED", new Meta(Category.LIMIT, false));
        table.put("ERROR_TOO_MANY_THREADS", new Meta(Category.LIMIT, true));
        table.put("ERROR_NO_SLOT_AVAILABLE", new Meta(Category.CAPACITY, true));
        table.put("ERROR_MAINTENANCE", new Meta(Category.SERVICE, false));
        result(table, "ERROR_TIMEOUT", "ERROR_TTL", "ERROR_TOKEN_EXPIRED", "ERROR_CAPTCHA_UNSOLVABLE");
        proxy(table, "ERROR_PROXY_REQUIRED", "ERROR_PROXY_NOT_AUTHORIZED", "ERROR_PROXY_BANNED");
        table.put("ERROR_PINGBACK_NOT_VERIFIED", new Meta(Category.PINGBACK, false));
        module(table, "ERROR_MODULE_ACCESS_DENIED", "ERROR_MODULE_NOT_TRAINED");
        return table;
    }

    private static void auth(Map<String, Meta> table, String... codes) {
        put(table, Category.AUTH, false, codes);
    }

    private static void request(Map<String, Meta> table, String... codes) {
        put(table, Category.REQUEST, false, codes);
    }

    private static void result(Map<String, Meta> table, String... codes) {
        put(table, Category.RESULT, false, codes);
    }

    private static void proxy(Map<String, Meta> table, String... codes) {
        put(table, Category.PROXY, false, codes);
    }

    private static void module(Map<String, Meta> table, String... codes) {
        put(table, Category.MODULE, false, codes);
    }

    private static void put(Map<String, Meta> table, Category category, boolean retryable, String... codes) {
        for (String code : codes) {
            table.put(code, new Meta(category, retryable));
        }
    }
}

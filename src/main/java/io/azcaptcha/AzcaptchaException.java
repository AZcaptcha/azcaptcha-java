package io.azcaptcha;

/** Typed error carrying the API code, category and taxonomy retryable flag. */
public class AzcaptchaException extends RuntimeException {
    public enum Category {
        PROCESSING, AUTH, BALANCE, REQUEST, LIMIT, CAPACITY,
        SERVICE, RESULT, PROXY, PINGBACK, MODULE, NETWORK, UNKNOWN
    }

    private final String code;
    private final Category category;
    private final boolean retryable;

    public AzcaptchaException(String code, Category category, boolean retryable, String message) {
        super(message == null || message.isEmpty() ? code : message);
        this.code = code;
        this.category = category;
        this.retryable = retryable;
    }

    public String code() {
        return code;
    }

    public Category category() {
        return category;
    }

    public boolean retryable() {
        return retryable;
    }
}

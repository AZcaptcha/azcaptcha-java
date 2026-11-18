package io.azcaptcha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

final class ErrorCodesTest {
    @Test
    void everySpecCodeMapsToTypedCategory() {
        List<?> codes = Fixtures.codes("errors");
        List<?> envelope = Fixtures.codes("envelope_codes");
        for (Object code : codes) {
            AzcaptchaException error = ErrorCodes.error((String) code);
            assertEquals(code, error.code());
            assertFalse(error.category() == AzcaptchaException.Category.UNKNOWN, (String) code);
        }
        assertTrue(ErrorCodes.taxonomy().containsKey((String) envelope.get(0)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void retryableFlagsMatchSpec() {
        Map<String, Object> spec = Fixtures.load("errors");
        Map<String, Object> errors = (Map<String, Object>) spec.get("errors");
        Map<String, Boolean> flags = ErrorCodes.taxonomy();
        errors.forEach((code, meta) -> {
            boolean expected = (Boolean) ((Map<String, Object>) meta).get("retryable");
            assertEquals(expected, flags.get(code), code);
            assertEquals(expected, ErrorCodes.error(code).retryable(), code);
        });
        Map<String, Object> envelope = (Map<String, Object>) spec.get("envelope_codes");
        envelope.forEach((code, meta) -> assertEquals(
                (Boolean) ((Map<String, Object>) meta).get("retryable"),
                ErrorCodes.error(code).retryable(), code));
    }

    @Test
    void unknownCodesAreForwardCompatible() {
        AzcaptchaException error = ErrorCodes.error("ERROR_FROM_THE_FUTURE");
        assertEquals(AzcaptchaException.Category.UNKNOWN, error.category());
        assertFalse(error.retryable());
    }
}

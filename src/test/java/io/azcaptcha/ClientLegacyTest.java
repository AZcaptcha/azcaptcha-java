package io.azcaptcha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

final class ClientLegacyTest {
    private final List<Duration> sleeps = new ArrayList<>();

    private AzcaptchaClient client(FakeTransport transport, int softId) {
        return new AzcaptchaClient("test-key", new AzcaptchaConfig()
                .transport(transport)
                .softId(softId)
                .sleeper(sleeps::add));
    }

    @Test
    void solvesImageFromFixture() {
        FakeTransport transport = new FakeTransport(Fixtures.body("submit-image"),
                Map.of("status", 1, "request", "wxyz"));
        assertEquals("wxyz", client(transport, 0).solveImage("PNG".getBytes(), null));

        FakeTransport.Call call = transport.calls.get(0);
        assertEquals("form", call.kind);
        assertEquals("/in.php", call.path);
        Map<?, ?> form = (Map<?, ?>) call.payload;
        assertEquals("base64", form.get("method"));
        assertEquals("test-key", form.get("key"));
        assertEquals("1", form.get("json"));
        assertEquals(Params.imageBody("PNG".getBytes()), form.get("body"));
    }

    @Test
    void submitsRecaptchaV2FromFixture() {
        FakeTransport transport = new FakeTransport(Fixtures.body("submit-recaptcha-v2"));
        String id = client(transport, 0).submit("userrecaptcha", Map.of(
                "googlekey", "6Le-wvkSAAAAAPBMRTvw0Q4Muexq9bi0DJwx_mJ-",
                "pageurl", "https://example.com/login"));
        assertEquals("1234567891", id);
        assertFalse(((Map<?, ?>) transport.calls.get(0).payload).containsKey("soft_id"));
    }

    @Test
    void sendsSoftIdWhenConfigured() {
        FakeTransport transport = new FakeTransport(Map.of("status", 1, "request", "1"));
        client(transport, 4242).submit("base64", Map.of("body", "AA=="));
        assertEquals("4242", ((Map<?, ?>) transport.calls.get(0).payload).get("soft_id"));
    }

    @Test
    void normalisesProxyFields() {
        FakeTransport transport = new FakeTransport(Map.of("status", 1, "request", "1"));
        client(transport, 0).submit("turnstile", Map.of(
                "sitekey", "0x4AAA", "pageurl", "https://e.com",
                "proxy", "socks5://u:p@1.2.3.4:1080"));
        Map<?, ?> form = (Map<?, ?>) transport.calls.get(0).payload;
        assertEquals("u:p@1.2.3.4:1080", form.get("proxy"));
        assertEquals("SOCKS5", form.get("proxytype"));
    }

    @Test
    void mapsZeroBalanceFixture() {
        FakeTransport transport = new FakeTransport(Fixtures.body("error-zero-balance"));
        AzcaptchaException error = assertThrows(AzcaptchaException.class,
                () -> client(transport, 0).submit("userrecaptcha", Map.of(
                        "googlekey", "k", "pageurl", "https://e.com")));
        assertEquals("ERROR_ZERO_BALANCE", error.code());
        assertEquals(AzcaptchaException.Category.BALANCE, error.category());
        assertFalse(error.retryable());
    }

    @Test
    void mapsNoSlotFixtureToRetryableCapacity() {
        FakeTransport transport = new FakeTransport(Fixtures.body("error-no-slot"));
        AzcaptchaException error = assertThrows(AzcaptchaException.class,
                () -> client(transport, 0).submit("turnstile", Map.of(
                        "sitekey", "k", "pageurl", "https://e.com")));
        assertEquals(AzcaptchaException.Category.CAPACITY, error.category());
        assertTrue(error.retryable());
    }

    @Test
    void readsPollFixtures() {
        FakeTransport ready = new FakeTransport(Fixtures.body("poll-ready-recaptcha"));
        assertEquals("03AGdBq26...", client(ready, 0).result("1234567891"));

        FakeTransport processing = new FakeTransport(Fixtures.body("poll-processing"));
        AzcaptchaException error = assertThrows(AzcaptchaException.class,
                () -> client(processing, 0).result("1234567891"));
        assertEquals("CAPCHA_NOT_READY", error.code());
    }

    @Test
    void waitsWithContractDelays() {
        FakeTransport transport = new FakeTransport(
                Fixtures.body("poll-processing"), Fixtures.body("poll-ready-recaptcha"));
        assertEquals("03AGdBq26...", client(transport, 0).waitResult("1234567891", Duration.ofSeconds(300)));
        assertEquals(List.of(Duration.ofSeconds(1)), sleeps);
    }

    @Test
    void backsOffOnNoSlot() {
        FakeTransport transport = new FakeTransport(
                Map.of("status", 0, "request", "ERROR_NO_SLOT_AVAILABLE"),
                Map.of("status", 1, "request", "token"));
        assertEquals("token", client(transport, 0).waitResult("1", Duration.ofSeconds(300)));
        assertEquals(List.of(Duration.ofSeconds(10)), sleeps);
    }

    @Test
    void raisesNonRetryableImmediately() {
        FakeTransport transport = new FakeTransport(
                Map.of("status", 0, "request", "ERROR_CAPTCHA_UNSOLVABLE"));
        AzcaptchaException error = assertThrows(AzcaptchaException.class,
                () -> client(transport, 0).waitResult("1", Duration.ofSeconds(300)));
        assertEquals(AzcaptchaException.Category.RESULT, error.category());
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void timesOut() {
        FakeTransport transport = new FakeTransport(
                Map.of("status", 0, "request", "CAPCHA_NOT_READY"));
        AzcaptchaException error = assertThrows(AzcaptchaException.class,
                () -> client(transport, 0).waitResult("1", Duration.ofMillis(1)));
        assertEquals("ERROR_TIMEOUT", error.code());
    }

    @Test
    void readsBalanceAndReportsBad() {
        FakeTransport transport = new FakeTransport(
                Map.of("status", 1, "request", 12.5),
                Map.of("status", 1, "request", "OK_REPORT_RECORDED"));
        AzcaptchaClient client = client(transport, 0);
        assertEquals(12.5, client.balance());
        assertTrue(client.reportBad("123"));
    }

    @Test
    void rejectsEmptyKey() {
        assertThrows(AzcaptchaException.class, () -> new AzcaptchaClient(""));
    }
}

package io.azcaptcha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

final class ClientModernTest {
    private final List<Duration> sleeps = new ArrayList<>();

    private AzcaptchaClient client(FakeTransport transport) {
        return new AzcaptchaClient("test-key", new AzcaptchaConfig()
                .transport(transport)
                .sleeper(sleeps::add));
    }

    @Test
    void createsTaskFromFixture() {
        FakeTransport transport = new FakeTransport(Fixtures.body("modern-create-task"));
        int id = client(transport).createTask("RecaptchaV2TaskProxyless", Map.of(
                "websiteURL", "https://example.com/login",
                "websiteKey", "6Le-wvkSAAAAAPBMRTvw0Q4Muexq9bi0DJwx_mJ-"));
        assertEquals(1234567892, id);
        FakeTransport.Call call = transport.calls.get(0);
        assertEquals("json", call.kind);
        assertEquals("/createTask", call.path);
        Map<?, ?> payload = (Map<?, ?>) call.payload;
        assertEquals("test-key", payload.get("clientKey"));
        assertEquals(Fixtures.task("modern-create-task"), payload.get("task"));
    }

    @Test
    void readsReadyTaskResult() {
        FakeTransport transport = new FakeTransport(Fixtures.body("modern-task-ready"));
        Map<String, Object> body = client(transport).getTaskResult(1234567892);
        assertEquals("ready", body.get("status"));
        assertEquals("03AGdBq26...", ((Map<?, ?>) body.get("solution")).get("gRecaptchaResponse"));
    }

    @Test
    void pollsUntilReady() {
        FakeTransport transport = new FakeTransport(
                Map.of("errorId", 0, "status", "processing"),
                Map.of("errorId", 0, "status", "ready", "solution", Map.of("token", "abc")));
        assertEquals(Map.of("token", "abc"), client(transport).waitTaskResult(1, Duration.ofSeconds(300)));
        assertEquals(List.of(Duration.ofSeconds(1)), sleeps);
    }

    @Test
    void mapsModernErrors() {
        FakeTransport transport = new FakeTransport(
                Map.of("errorId", 12, "errorCode", "ERROR_ZERO_BALANCE"));
        AzcaptchaException error = assertThrows(AzcaptchaException.class,
                () -> client(transport).createTask("RecaptchaV2TaskProxyless", null));
        assertEquals(AzcaptchaException.Category.BALANCE, error.category());
    }

    @Test
    void timesOut() {
        FakeTransport transport = new FakeTransport(Map.of("errorId", 0, "status", "processing"));
        AzcaptchaException error = assertThrows(AzcaptchaException.class,
                () -> client(transport).waitTaskResult(1, Duration.ofMillis(1)));
        assertEquals("ERROR_TIMEOUT", error.code());
    }

    @Test
    void asyncFacadeRunsOnExecutor() throws Exception {
        FakeTransport transport = new FakeTransport(Map.of("errorId", 0, "status", "ready",
                "solution", Map.of("token", "abc")));
        Map<String, Object> solution = client(transport)
                .async().getTaskResult(1).get(5, TimeUnit.SECONDS);
        assertEquals("ready", solution.get("status"));
    }
}

# AZcaptcha Java SDK

Official Java client for [AZcaptcha](https://azcaptcha.com) — image captchas,
reCAPTCHA v2/v3, hCaptcha, Cloudflare Turnstile, FunCaptcha and GeeTest.

- Java 11+, `java.net.http`, no annotations or code generation.
- Sync client plus a `CompletableFuture` facade.
- 2Captcha-compatible surface (`solve`, `result`, `balance`, `reportBad`) plus the
  modern JSON API (`createTask`, `getTaskResult`).
- Typed `AzcaptchaException` with `code`, `category` and `retryable`
  ([spec-v1.0.0](https://github.com/AZcaptcha/sdk-spec)).
- Contract polling with `NO_SLOT` backoff and per-type deadlines.

## Install

Maven:

```xml
<dependency>
  <groupId>io.azcaptcha</groupId>
  <artifactId>azcaptcha-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'io.azcaptcha:azcaptcha-sdk:1.0.0'
```

## Quickstart

```java
import io.azcaptcha.AzcaptchaClient;
import java.nio.file.Path;
import java.util.Map;

var client = new AzcaptchaClient("YOUR_API_KEY");

// reCAPTCHA v2
String token = client.solveRecaptchaV2(
    "6Le-wvkSAAAAAPBMRTvw0Q4Muexq9bi0DJwx_mJ-",
    "https://example.com/login");

// Image captcha (bytes, file path or base64/data: URI)
String answer = client.solveImageFile(Path.of("captcha.png"), null);

// hCaptcha / Turnstile
String h = client.solveHCaptcha("SITEKEY", "https://example.com/");
String t = client.solveTurnstile("0x4AAAAAAA...", "https://example.com/");
```

### Low-level (2Captcha style)

```java
String taskId = client.submit("userrecaptcha", Map.of(
    "googlekey", "SITEKEY",
    "pageurl", "https://example.com/"));
String token = client.waitResult(taskId, Duration.ofSeconds(300));
```

### Modern JSON API

```java
int taskId = client.createTask("RecaptchaV2TaskProxyless", Map.of(
    "websiteURL", "https://example.com/login",
    "websiteKey", "SITEKEY"));
Map<String, Object> solution = client.waitTaskResult(taskId, Duration.ofSeconds(300));
System.out.println(solution.get("gRecaptchaResponse"));
```

### Async

```java
CompletableFuture<String> token = client.async().solveTurnstile(sitekey, pageurl);
```

## Proxies

```java
client.solveRecaptchaV2(sitekey, pageurl, false, false,
    Map.of("proxy", "socks5://user:pass@1.2.3.4:1080"));
```

Any accepted spelling works (`scheme://user:pass@host:port`,
`host:port:user:pass`, `user:pass@host:port`). The scheme is never sent
upstream; `proxytype` is derived (`http`/`https` → `HTTP`, `socks4`, `socks5`).

## Errors

```java
try {
    String token = client.solveTurnstile(sitekey, pageurl);
} catch (AzcaptchaException error) {
    switch (error.category()) {
        case BALANCE:  // ERROR_ZERO_BALANCE
        case CAPACITY: // ERROR_NO_SLOT_AVAILABLE — retry with backoff
        case RESULT:   // unsolvable / timeout / expired
        default: throw error;
    }
}
```

`waitResult` retries only codes marked retryable and backs off 10s ×1.5 up to
60s.

## Timeouts

Defaults match server settings: image 120s, hCaptcha 120s, Turnstile 180s,
reCAPTCHA and others 300s. Override with
`client.solve(method, params, Duration.ofSeconds(90))`.

## Custom transport

```java
var client = new AzcaptchaClient("KEY", new AzcaptchaConfig()
    .transport(myTransport)
    .sleeper(mySleeper)
    .executor(myExecutor));
```

`Transport` requires `postForm`, `postJson` and `get`.

## Links

- API contract: <https://github.com/AZcaptcha/sdk-spec>
- Docs: <https://azcaptcha.com/docs>
- Java guide: <https://azcaptcha.com/lang/java>
- Changelog: [CHANGELOG.md](CHANGELOG.md)

## License

MIT — see [LICENSE](LICENSE).

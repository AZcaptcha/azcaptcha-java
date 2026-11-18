package io.azcaptcha;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/** Overrides for the client; the defaults are production-ready. */
public final class AzcaptchaConfig {
    String host = "https://azcaptcha.com";
    Duration httpTimeout = Duration.ofSeconds(30);
    int softId = 0;
    Transport transport;
    Sleeper sleeper = duration -> Thread.sleep(duration.toMillis());
    Executor executor = ForkJoinPool.commonPool();

    public AzcaptchaConfig host(String value) {
        this.host = value;
        return this;
    }

    public AzcaptchaConfig httpTimeout(Duration value) {
        this.httpTimeout = value;
        return this;
    }

    public AzcaptchaConfig softId(int value) {
        this.softId = value;
        return this;
    }

    public AzcaptchaConfig transport(Transport value) {
        this.transport = value;
        return this;
    }

    public AzcaptchaConfig sleeper(Sleeper value) {
        this.sleeper = value;
        return this;
    }

    public AzcaptchaConfig executor(Executor value) {
        this.executor = value;
        return this;
    }
}

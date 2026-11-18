package io.azcaptcha;

import java.time.Duration;

/** Injectable sleep so tests never wait. */
@FunctionalInterface
public interface Sleeper {
    void sleep(Duration duration) throws InterruptedException;
}

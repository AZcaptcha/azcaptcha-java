package io.azcaptcha;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ParamsTest {
    @Test
    void normalisesProxies() {
        assertArrayEquals(new String[]{"1.2.3.4:8080", "HTTP"}, Params.normalizeProxy("1.2.3.4:8080"));
        assertArrayEquals(new String[]{"user:pass@1.2.3.4:8080", "HTTP"},
                Params.normalizeProxy("user:pass@1.2.3.4:8080"));
        assertArrayEquals(new String[]{"user:pass@1.2.3.4:8080", "HTTP"},
                Params.normalizeProxy("1.2.3.4:8080:user:pass"));
        assertArrayEquals(new String[]{"u:p@1.2.3.4:1080", "SOCKS5"},
                Params.normalizeProxy("socks5://u:p@1.2.3.4:1080"));
        assertArrayEquals(new String[]{"u:p@1.2.3.4:1080", "SOCKS4"},
                Params.normalizeProxy("socks4://u:p@1.2.3.4:1080"));
        assertArrayEquals(new String[]{"u:p@1.2.3.4:8080", "HTTP"},
                Params.normalizeProxy("https://u:p@1.2.3.4:8080"));
    }

    @Test
    void rejectsProxyWithoutPort() {
        assertThrows(IllegalArgumentException.class, () -> Params.normalizeProxy("1.2.3.4"));
    }

    @Test
    void encodesImages(@TempDir Path dir) throws Exception {
        assertEquals("UE5H", Params.imageBody("PNG".getBytes()));
        assertEquals("AAAA", Params.imageBase64("data:image/png;base64,AAAA"));
        assertEquals("iVBORw0KGgo=", Params.imageBase64("iVBORw0KGgo=\n"));

        Path file = dir.resolve("captcha.png");
        Files.write(file, "PNG".getBytes());
        assertEquals("UE5H", Params.imageFile(file));

        assertThrows(AzcaptchaException.class, () -> Params.imageFile(dir.resolve("missing.png")));
    }
}

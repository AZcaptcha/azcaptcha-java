package io.azcaptcha;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Proxy and image parameter normalisation shared by the client. */
public final class Params {
    public static final Map<String, Duration> DEFAULT_TIMEOUTS = buildTimeouts();

    private Params() {
    }

    /**
     * Accepts scheme://user:pass@host:port, host:port:user:pass and
     * user:pass@host:port; returns {proxy, proxytype}. The scheme is never sent.
     */
    public static String[] normalizeProxy(String proxy) {
        String raw = proxy.trim();
        String type = "HTTP";
        int scheme = raw.indexOf("://");
        if (scheme >= 0) {
            switch (raw.substring(0, scheme).toLowerCase()) {
                case "socks4": type = "SOCKS4"; break;
                case "socks5": type = "SOCKS5"; break;
                default: type = "HTTP"; break;
            }
            raw = raw.substring(scheme + 3);
        }
        String[] parts = raw.split(":");
        if (parts.length == 4 && parts[1].matches("\\d+")) {
            raw = parts[2] + ":" + parts[3] + "@" + parts[0] + ":" + parts[1];
        } else if (!raw.contains("@")) {
            int portIdx = raw.lastIndexOf(':');
            if (portIdx < 0 || !raw.substring(portIdx + 1).matches("\\d+")) {
                throw new IllegalArgumentException("proxy must include a numeric port");
            }
        }
        return new String[]{raw, type};
    }

    public static String imageBody(byte[] image) {
        return Base64.getEncoder().encodeToString(image);
    }

    public static String imageFile(Path path) {
        try {
            return imageBody(Files.readAllBytes(path));
        } catch (IOException error) {
            throw ErrorCodes.error("ERROR_IMAGE_NOT_FOUND", error.getMessage());
        }
    }

    public static String imageBase64(String image) {
        String value = image.trim();
        int comma = value.indexOf(',');
        if (value.startsWith("data:") && comma >= 0) {
            value = value.substring(comma + 1);
        }
        return value.replaceAll("\\s+", "");
    }

    static Map<String, Object> merge(Map<String, Object> base, Object... pairs) {
        Map<String, Object> out = new LinkedHashMap<>(base);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            out.put((String) pairs[i], pairs[i + 1]);
        }
        return out;
    }

    private static Map<String, Duration> buildTimeouts() {
        Map<String, Duration> map = new LinkedHashMap<>();
        put(map, 120, "base64", "grid");
        put(map, 300, "userrecaptcha", "recaptcha_v3");
        put(map, 120, "hcaptcha", "hcaptcha_enterprise");
        put(map, 180, "turnstile");
        put(map, 300, "funcaptcha", "geetest", "geetest_v4", "mtcaptcha", "friendlycaptcha",
                "altcha", "cf_clearance", "aws_waf", "filecrypt");
        return map;
    }

    private static void put(Map<String, Duration> map, int seconds, String... methods) {
        for (String method : methods) {
            map.put(method, Duration.ofSeconds(seconds));
        }
    }

    static Duration timeoutFor(String method) {
        return DEFAULT_TIMEOUTS.getOrDefault(method, Duration.ofSeconds(300));
    }
}

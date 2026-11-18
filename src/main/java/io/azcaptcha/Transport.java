package io.azcaptcha;

import java.util.Map;

/** Minimal HTTP surface the client needs; tests provide a fake. */
public interface Transport {
    Map<String, Object> postForm(String path, Map<String, String> form);

    Map<String, Object> postJson(String path, Object payload);

    Map<String, Object> get(String path, Map<String, String> query);
}

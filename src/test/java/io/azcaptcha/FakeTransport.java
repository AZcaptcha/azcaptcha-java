package io.azcaptcha;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Replays canned JSON bodies and records every call. */
final class FakeTransport implements Transport {
    static final class Call {
        final String kind;
        final String path;
        final Object payload;

        Call(String kind, String path, Object payload) {
            this.kind = kind;
            this.path = path;
            this.payload = payload;
        }
    }

    private final Deque<Map<String, Object>> responses = new ArrayDeque<>();
    final List<Call> calls = new ArrayList<>();

    @SafeVarargs
    FakeTransport(Map<String, Object>... bodies) {
        for (Map<String, Object> body : bodies) {
            responses.add(body);
        }
    }

    @Override
    public Map<String, Object> postForm(String path, Map<String, String> form) {
        return next("form", path, new LinkedHashMap<>(form));
    }

    @Override
    public Map<String, Object> postJson(String path, Object payload) {
        return next("json", path, payload);
    }

    @Override
    public Map<String, Object> get(String path, Map<String, String> query) {
        return next("get", path, new LinkedHashMap<>(query));
    }

    private Map<String, Object> next(String kind, String path, Object payload) {
        calls.add(new Call(kind, path, payload));
        Map<String, Object> body = responses.poll();
        if (body == null) {
            throw new IllegalStateException("unexpected call: " + kind + " " + path);
        }
        return body;
    }
}

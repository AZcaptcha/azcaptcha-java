package io.azcaptcha;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.azcaptcha.AzcaptchaException.Category;

/** Default transport — java.net.http, JSON envelopes only. */
public final class HttpTransport implements Transport {
    private static final String USER_AGENT = "azcaptcha-java/1.0.0";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final HttpClient client;
    private final String baseUrl;

    public HttpTransport(String baseUrl, Duration timeout) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public Map<String, Object> postForm(String path, Map<String, String> form) {
        return send(builder(path)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encode(form)))
                .build());
    }

    @Override
    public Map<String, Object> postJson(String path, Object payload) {
        String json;
        try {
            json = MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException error) {
            throw ErrorCodes.error("ERROR_BAD_PARAMETERS", error.getMessage());
        }
        return send(builder(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    @Override
    public Map<String, Object> get(String path, Map<String, String> query) {
        String suffix = query.isEmpty() ? "" : "?" + encode(query);
        return send(builder(path + suffix).GET().build());
    }

    private HttpRequest.Builder builder(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT);
    }

    private Map<String, Object> send(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AzcaptchaException("ERROR_NETWORK", Category.NETWORK, true, error.getMessage());
        } catch (IOException error) {
            throw new AzcaptchaException("ERROR_NETWORK", Category.NETWORK, true, error.getMessage());
        }
        Map<String, Object> body;
        try {
            body = MAPPER.readValue(response.body(), MAP_TYPE);
        } catch (JsonProcessingException error) {
            throw ErrorCodes.error("ERROR_BAD_RESPONSE", response.body());
        }
        if (response.statusCode() >= 500) {
            throw ErrorCodes.error("ERROR_HTTP_5XX", "HTTP " + response.statusCode());
        }
        return body;
    }

    private static String encode(Map<String, String> values) {
        return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}

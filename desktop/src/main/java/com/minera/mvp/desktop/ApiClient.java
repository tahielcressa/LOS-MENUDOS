package com.minera.mvp.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cliente REST sencillo de la app de escritorio contra el backend local. */
public class ApiClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    private final String base;
    private String token = "";

    public ApiClient(String base) {
        this.base = base;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> login(String email, String password) throws Exception {
        String body = JSON.writeValueAsString(Map.of("email", email, "password", password));
        HttpResponse<String> res = post("/auth/login", body, false);
        Map<String, Object> json = JSON.readValue(res.body(), Map.class);
        if (res.statusCode() != 200) {
            throw new RuntimeException(msg(json));
        }
        return json;
    }

    public boolean hasToken() {
        return !token.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String path) throws Exception {
        HttpResponse<String> res = send(request(path).GET().build());
        if (res.statusCode() != 200) {
            throw new RuntimeException(msg(JSON.readValue(res.body(), Map.class)));
        }
        return JSON.readValue(res.body(), Map.class);
    }

    @SuppressWarnings("unchecked")
    public List<Object> getList(String path) throws Exception {
        HttpResponse<String> res = send(request(path).GET().build());
        if (res.statusCode() != 200) {
            throw new RuntimeException(msg(JSON.readValue(res.body(), Map.class)));
        }
        return JSON.readValue(res.body(), new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> postJson(String path, Map<String, Object> payload) throws Exception {
        String body = JSON.writeValueAsString(payload);
        HttpResponse<String> res = post(path, body, true);
        if (res.statusCode() != 200 && res.statusCode() != 201) {
            throw new RuntimeException(msg(JSON.readValue(res.body(), Map.class)));
        }
        return JSON.readValue(res.body(), Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadFile(String path, Path file) throws Exception {
        byte[] content = Files.readAllBytes(file);
        String boundary = "----" + UUID.randomUUID();
        String filename = file.getFileName().toString();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(content);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest req = request(path)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();

        HttpResponse<String> res = send(req);
        if (res.statusCode() != 200 && res.statusCode() != 201) {
            throw new RuntimeException(msg(JSON.readValue(res.body(), Map.class)));
        }
        return JSON.readValue(res.body(), Map.class);
    }

    public boolean download(String path, Path dest) throws Exception {
        HttpRequest req = request(path).GET().build();
        HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) {
            return false;
        }
        try (InputStream in = res.body()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    private HttpResponse<String> post(String path, String body, boolean auth) throws Exception {
        HttpRequest req = request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return send(req);
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(base + path));
        if (!token.isEmpty()) {
            b.header("Authorization", "Bearer " + token);
        }
        return b;
    }

    private HttpResponse<String> send(HttpRequest req) throws Exception {
        return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String msg(Object json) {
        if (json instanceof Map<?, ?> m && m.get("message") != null) {
            return String.valueOf(m.get("message"));
        }
        return "Error de comunicacion con el servidor";
    }
}
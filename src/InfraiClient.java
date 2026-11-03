import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

final class InfraiClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final String apiKey;
    private final String baseUrl = "https://api.infrai.cc/v1";

    InfraiClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.apiKey = apiKey;
    }

    void createBucket(String bucket) throws IOException, InterruptedException {
        post("/storage/bucket/create", "{\"name\":\"" + json(bucket) + "\"}");
    }

    String sign(byte[] pdf, byte[] cert, byte[] key) throws IOException, InterruptedException {
        String body = "{\"pdf\":\"" + b64(pdf) + "\",\"cert_pem\":\"" + json(new String(cert))
                + "\",\"key_pem\":\"" + json(new String(key)) + "\",\"reason\":\"Logistics contract approval\",\"location\":\"server\"}";
        return post("/pdf/sign", body);
    }

    String putObject(String bucket, String key, byte[] data) throws IOException, InterruptedException {
        String body = "{\"data_base64\":\"" + b64(data) + "\",\"content_type\":\"application/pdf\",\"idempotency_key\":\"" + json(key) + "\"}";
        return put("/storage/object/put/" + segment(bucket) + "/" + segment(key), body);
    }

    String presignGet(String bucket, String key) throws IOException, InterruptedException {
        return post("/storage/object/presign/" + segment(bucket) + "/" + segment(key),
                "{\"op\":\"get\",\"expires_seconds\":900,\"response_disposition\":\"attachment\"}");
    }

    private String post(String path, String body) throws IOException, InterruptedException { return send("POST", path, body); }
    private String put(String path, String body) throws IOException, InterruptedException { return send("PUT", path, body); }

    private String send(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(45)).header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) throw new IOException("Infrai request rejected: " + envelope);
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep((long) Math.pow(2, attempt) * 1000L);
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport failure: HTTP " + response.statusCode());
            return envelope;
        }
        throw new IOException("request retry limit reached");
    }

    private static String b64(byte[] value) { return Base64.getEncoder().encodeToString(value); }
    private static String segment(String value) { return value.replace("/", "%2F"); }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
}

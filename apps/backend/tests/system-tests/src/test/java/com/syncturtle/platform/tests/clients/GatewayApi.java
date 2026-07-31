package com.syncturtle.platform.tests.clients;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

public final class GatewayApi {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final URI actuatorBaseUri;
    private final HttpClient client;

    public GatewayApi(String baseUrl) {
        this.actuatorBaseUri = createActuatorBaseUri(baseUrl);

        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public HttpResponse<String> getActuatorRaw(String actuatorPath) throws IOException, InterruptedException {
        String normalizedPath = normalizeActuatorPath(actuatorPath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(actuatorBaseUri.resolve(normalizedPath))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static URI createActuatorBaseUri(String rawBaseUrl) {
        String baseUrl = Objects.requireNonNull(rawBaseUrl, "baseUrl must not be null").trim();

        if (baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }

        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return URI.create(baseUrl + "/actuator/");
    }

    private static String normalizeActuatorPath(String rawPath) {
        String path = Objects.requireNonNull(rawPath, "actuatorPath must not be null").trim();

        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (path.isEmpty()) {
            throw new IllegalArgumentException("actuatorPath must not be blank");
        }

        return path;
    }

}

package com.syncturtle.platform.tests.clients;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class GatewayApi {

    private final String baseUrl;
    private final HttpClient client;

    public GatewayApi(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public HttpResponse<String> getActuatorRaw(String actuatorPath) throws IOException, InterruptedException {
        String path = actuatorPath.startsWith("/") ? actuatorPath : "/" + actuatorPath;

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/actuator" + path))
                .timeout(Duration.ofSeconds(10)).GET().header("Accept", "application/json").build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

}

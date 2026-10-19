package com.syncturtle.platform.gateway.security.browser;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

/** Uses the same explicit origins as credentialed Gateway CORS. */
public final class GatewayBrowserOriginPolicy {

    private final Set<String> trustedOrigins;

    public GatewayBrowserOriginPolicy(GlobalCorsProperties properties) {
        CorsConfiguration cors = properties.getCorsConfigurations().get("/**");
        List<String> origins = cors == null ? null : cors.getAllowedOrigins();

        if (origins == null || origins.isEmpty() || !Boolean.TRUE.equals(cors.getAllowCredentials())) {
            throw new IllegalStateException("Credentialed browser CORS requires explicit allowed origins");
        }

        Set<String> validated = new HashSet<>();
        for (String origin : origins) {
            URI uri;

            try {
                uri = URI.create(origin);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Invalid browser origin", exception);
            }

            if ((!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || "*".equals(origin) || !validated.add(origin)) {
                throw new IllegalStateException("Browser origins must be exact, unique HTTP(S) origins");
            }
        }

        this.trustedOrigins = Set.copyOf(validated);
    }

    public boolean accepts(ServerWebExchange exchange) {
        List<String> values = exchange.getRequest().getHeaders().get(HttpHeaders.ORIGIN);
        return values != null && values.size() == 1 && trustedOrigins.contains(values.getFirst());
    }

}

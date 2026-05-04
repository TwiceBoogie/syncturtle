package com.syncturtle.platform.gateway.configurations.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.platform.gateway.configurations.properties.GatewayPassportProperties;
import com.syncturtle.platform.gateway.filters.web.PassportSessionValidationWebFilter;
import com.syncturtle.platform.gateway.support.CookieOrBearerServerAuthenticationConverter;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayPassportProperties.class)
public class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            CookieOrBearerServerAuthenticationConverter tokenConverter,
            Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter,
            PassportSessionValidationWebFilter sessionValidationWebFilter) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> unauthorized(exchange))
                        .accessDeniedHandler((exchange, e) -> forbidden(exchange)))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health").permitAll()

                        .pathMatchers(HttpMethod.GET,
                                "/api/instances",
                                "/api/instances/admins/session")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST,
                                "/api/instances/admins/sign-in",
                                "/api/instances/admins/sign-up",
                                "/api/instances/admins/sign-up-screen-visited",
                                "/api/instances/admins/sign-out")
                        .permitAll()

                        .pathMatchers(
                                "/auth/sign-in/**",
                                "/auth/sign-up/**",
                                "/auth/sign-out/**",
                                "/auth/get-csrf-token/**",
                                "/auth/refresh",
                                "/auth/email-check/**",
                                "/auth/magic-generate/**",
                                "/auth/magic-sign-in/**",
                                "/auth/magic-sign-up/**",
                                "/auth/forgot-password/**",
                                "/auth/reset-password/**",
                                "/auth/google/**",
                                "/auth/github/**",
                                "/auth/gitlab/**",
                                "/oauth2/**")
                        .permitAll()

                        .pathMatchers("/api/users/**").authenticated()
                        .pathMatchers("/api/workspaces/**").authenticated()
                        .pathMatchers("/api/instances/**").authenticated()

                        .pathMatchers(
                                "/auth/change-password/**",
                                "/auth/set-password/**")
                        .authenticated()

                        .anyExchange().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenConverter(tokenConverter)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterAfter(sessionValidationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwtSetUri,
            GatewayPassportProperties properties) {

        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwtSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators
                .createDefaultWithIssuer(properties.getIssuer());

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience()
                .contains(properties.getAudience())
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                OAuth2ErrorCodes.INVALID_TOKEN,
                                "Invalid audience",
                                null));

        OAuth2TokenValidator<Jwt> tokenUseValidator = jwt -> "access".equals(jwt.getClaimAsString("token_use"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_TOKEN,
                        "token_use must be access",
                        null));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator,
                audienceValidator,
                tokenUseValidator));

        return decoder;
    }

    @Bean
    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        return jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();

            List<String> roles = jwt.getClaimAsStringList("roles");

            if (roles != null) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }

            return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        applyCorsHeaders(exchange);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"ok":false,"error":"UNAUTHORIZED","message":"Authentication required"}
                """;

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        applyCorsHeaders(exchange);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"ok":false,"error":"FORBIDDEN","message":"Access denied"}
                """;

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    private void applyCorsHeaders(ServerWebExchange exchange) {
        String origin = exchange.getRequest().getHeaders().getOrigin();

        if ("http://localhost:3001".equals(origin) || "http://localhost:3000".equals(origin)) {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.setAccessControlAllowOrigin(origin);
            headers.setAccessControlAllowCredentials(true);
            headers.add("Vary", "Origin");
        }
    }
}
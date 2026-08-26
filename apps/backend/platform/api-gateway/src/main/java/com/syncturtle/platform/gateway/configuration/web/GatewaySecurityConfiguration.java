package com.syncturtle.platform.gateway.configuration.web;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.platform.gateway.configuration.property.GatewayClientMetadataProperties;
import com.syncturtle.platform.gateway.configuration.property.GatewayPassportProperties;
import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.security.CookieOrBearerServerAuthenticationConverter;
import com.syncturtle.platform.gateway.security.GatewayRouteAuthorizationManager;
import com.syncturtle.platform.gateway.security.GatewayRouteSecurityPolicy;
import com.syncturtle.platform.gateway.security.PassportAuthenticationMetrics;
import com.syncturtle.platform.gateway.security.PassportJwtAuthenticationConverter;
import com.syncturtle.platform.gateway.type.GatewayRouteSecurityCategory;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import reactor.core.publisher.Mono;

@EnableWebFluxSecurity
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ GatewayPassportProperties.class, GatewayClientMetadataProperties.class })
public class GatewaySecurityConfiguration {

    private final PassportAuthenticationMetrics metrics;

    public GatewaySecurityConfiguration(PassportAuthenticationMetrics metrics) {
        this.metrics = metrics;
    }

    @Bean
    @Order(0)
    SecurityWebFilterChain publicSecurityWebFilterChain(
            ServerHttpSecurity http,
            GatewayRouteSecurityPolicy routePolicy) {
        configureBase(http);
        return http
                .securityMatcher(routePolicy.matcher(GatewayRouteSecurityCategory.PUBLIC))
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }

    @Bean
    @Order(1)
    SecurityWebFilterChain optionalAuthenticationSecurityWebFilterChain(
            ServerHttpSecurity http,
            GatewayRouteSecurityPolicy routePolicy,
            CookieOrBearerServerAuthenticationConverter credentialConverter,
            PassportJwtAuthenticationConverter jwtAuthenticationConverter) {
        configureBase(http);
        configurePassport(http, credentialConverter, jwtAuthenticationConverter);
        return http
                .securityMatcher(routePolicy.matcher(GatewayRouteSecurityCategory.OPTIONAL_AUTH))
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityWebFilterChain protectedSecurityWebFilterChain(
            ServerHttpSecurity http,
            GatewayRouteSecurityPolicy routePolicy,
            GatewayRouteAuthorizationManager authorizationManager,
            CookieOrBearerServerAuthenticationConverter credentialConverter,
            PassportJwtAuthenticationConverter jwtAuthenticationConverter) {
        configureBase(http);
        configurePassport(http, credentialConverter, jwtAuthenticationConverter);
        return http
                .securityMatcher(routePolicy.matcher(GatewayRouteSecurityCategory.PROTECTED))
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().access(authorizationManager))
                .build();
    }

    @Bean
    @Order(3)
    SecurityWebFilterChain defaultDenySecurityWebFilterChain(ServerHttpSecurity http) {
        configureBase(http);
        return http
                .authorizeExchange(exchanges -> exchanges.anyExchange().denyAll())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, exception) -> {
                            metrics.rejected(PassportAuthenticationFailureReason.DEFAULT_DENY);
                            return writeJson(
                                    exchange,
                                    HttpStatus.FORBIDDEN,
                                    "FORBIDDEN",
                                    "Route is not available.");
                        })
                        .accessDeniedHandler((exchange, exception) -> {
                            metrics.rejected(PassportAuthenticationFailureReason.DEFAULT_DENY);
                            return writeJson(
                                    exchange,
                                    HttpStatus.FORBIDDEN,
                                    "FORBIDDEN",
                                    "Route is not available.");
                        }))
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwtSetUri,
            OAuth2TokenValidator<Jwt> passportJwtValidator) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(jwtSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(passportJwtValidator);
        return decoder;
    }

    @Bean
    OAuth2TokenValidator<Jwt> passportJwtValidator(GatewayPassportProperties properties) {
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
                        "Invalid token use",
                        null));

        return new DelegatingOAuth2TokenValidator<>(
                issuerValidator,
                audienceValidator,
                tokenUseValidator);
    }

    @Bean
    Clock gatewayClock() {
        return Clock.systemUTC();
    }

    private void configureBase(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(this::authenticationFailure)
                        .accessDeniedHandler((exchange, exception) -> writeJson(
                                exchange,
                                HttpStatus.FORBIDDEN,
                                "FORBIDDEN",
                                "Access denied.")));
    }

    private void configurePassport(
            ServerHttpSecurity http,
            CookieOrBearerServerAuthenticationConverter credentialConverter,
            PassportJwtAuthenticationConverter jwtAuthenticationConverter) {
        http.oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenConverter(credentialConverter)
                .authenticationEntryPoint(this::authenticationFailure)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
    }

    private Mono<Void> authenticationFailure(
            ServerWebExchange exchange,
            AuthenticationException exception) {
        PassportAuthenticationException passportFailure = findPassportFailure(exception);
        if (passportFailure != null) {
            PassportAuthenticationFailureReason reason = passportFailure.getReason();
            metrics.rejected(reason);
            if (reason.isOperationalFailure()) {
                return writeJson(
                        exchange,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AUTHENTICATION_UNAVAILABLE",
                        "Authentication is temporarily unavailable.");
            }
        } else if (exception instanceof OAuth2AuthenticationException) {
            metrics.rejected(PassportAuthenticationFailureReason.JWT_REJECTED);
        } else {
            metrics.rejected(PassportAuthenticationFailureReason.AUTHENTICATION_REQUIRED);
        }

        return writeJson(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required.");
    }

    private PassportAuthenticationException findPassportFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof PassportAuthenticationException passportException) {
                return passportException;
            }
            current = current.getCause();
        }
        return null;
    }

    private Mono<Void> writeJson(
            ServerWebExchange exchange,
            HttpStatus status,
            String error,
            String message) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"ok":false,"error":"%s","message":"%s"}
                """.formatted(error, message);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

}
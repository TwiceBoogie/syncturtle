package com.syncturtle.common.security.provenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.exception.RequestProvenanceException;

class RequestProvenanceOncePerRequestFilterTest {

    private RequestProvenanceVerifier verifier;
    private RequestProvenanceOncePerRequestFilter filter;
    private RequestProvenanceClaims gatewayClaims;

    @BeforeEach
    void setup() {
        verifier = mock(RequestProvenanceVerifier.class);
        RequestProvenanceCanonicalizer canonicalizer = new RequestProvenanceCanonicalizer();
        filter = new RequestProvenanceOncePerRequestFilter(
                verifier,
                new RequestProvenanceRoutePolicy(RequestProvenancePolicy.userService(), canonicalizer),
                canonicalizer);
        gatewayClaims = claims("api-gateway");
    }

    @Nested
    class FilteringTests {

        @Test
        void acceptsVerifiedAuthorizedRequestBeforeContinuingTheChain() throws Exception {
            // arrange
            MockHttpServletRequest request = request("/api/users/me");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            // conditions
            when(verifier.verify(eq("assertion"), eq("GET"), eq("/api/users/me"), eq(null), any()))
                    .thenReturn(gatewayClaims);
            // act
            filter.doFilter(request, response, chain);
            // assert
            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(request.getAttribute(RequestProvenanceOncePerRequestFilter.VERIFIED_CLAIMS_ATTRIBUTE))
                    .isSameAs(gatewayClaims);
            // verify
        }

        @Test
        void rejectsUnsignedForgedAndDuplicateAssertionsWithGeneric401() throws Exception {
            // arrange
            MockHttpServletRequest unsigned = new MockHttpServletRequest("GET", "/api/users/me");
            unsigned.addHeader(GatewayHeaders.HDR_AUTH_USER_ID, "forged-user");
            MockHttpServletResponse unsignedResponse = new MockHttpServletResponse();
            MockHttpServletRequest duplicate = request("/api/users/me");
            duplicate.addHeader(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE, "second");
            MockHttpServletResponse duplicateResponse = new MockHttpServletResponse();
            // conditions
            // act
            filter.doFilter(unsigned, unsignedResponse, new MockFilterChain());
            filter.doFilter(duplicate, duplicateResponse, new MockFilterChain());
            // assert
            assertThat(unsignedResponse.getStatus()).isEqualTo(401);
            assertThat(unsignedResponse.getContentAsString()).contains("UNTRUSTED_REQUEST_PROVENANCE");
            assertThat(duplicateResponse.getStatus()).isEqualTo(401);
            // verify
            verifyNoInteractions(verifier);
        }

        @Test
        void rejectsSingleFakeAssertionWithoutEstablishingTrustedContextOrCallingTheChain() throws Exception {
            // arrange
            MockHttpServletRequest request = request("/api/users/me");
            request.addHeader(GatewayHeaders.HDR_AUTH_USER_ID, "forged-user");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            // conditions
            when(verifier.verify(eq("assertion"), eq("GET"), eq("/api/users/me"), eq(null), any()))
                    .thenThrow(new RequestProvenanceException("bad signature"));
            // act
            filter.doFilter(request, response, chain);
            // assert
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(chain.getRequest()).isNull();
            assertThat(request.getAttribute(RequestProvenanceOncePerRequestFilter.VERIFIED_CLAIMS_ATTRIBUTE))
                    .isNull();
            // verify
        }

        @Test
        void rejectsWrongIssuerRouteAndUnauthorizedActorHeader() throws Exception {
            // arrange
            MockHttpServletRequest wrongIssuer = request("/api/users/me");
            MockHttpServletResponse wrongIssuerResponse = new MockHttpServletResponse();
            // conditions
            when(verifier.verify(eq("assertion"), eq("GET"), eq("/api/users/me"), eq(null), any()))
                    .thenReturn(claims("instance-service"));
            // act
            filter.doFilter(wrongIssuer, wrongIssuerResponse, new MockFilterChain());
            // arrange
            MockHttpServletRequest actor = request("/api/users/me");
            actor.addHeader(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID, "user-1");
            MockHttpServletResponse actorResponse = new MockHttpServletResponse();
            // conditions
            when(verifier.verify(eq("assertion"), eq("GET"), eq("/api/users/me"), eq(null), any()))
                    .thenReturn(gatewayClaims);
            // act
            filter.doFilter(actor, actorResponse, new MockFilterChain());
            // assert
            assertThat(wrongIssuerResponse.getStatus()).isEqualTo(401);
            assertThat(actorResponse.getStatus()).isEqualTo(401);
            // verify
        }

        @Test
        void allowsNarrowHealthExemptionOnlyWhenItCarriesNoTrustedHeaders() throws Exception {
            // arrange
            MockHttpServletRequest health = new MockHttpServletRequest("GET", "/actuator/health/readiness");
            MockFilterChain chain = new MockFilterChain();
            // conditions
            // act
            filter.doFilter(health, new MockHttpServletResponse(), chain);
            // assert
            assertThat(chain.getRequest()).isSameAs(health);
            // arrange
            MockHttpServletRequest poisoned = new MockHttpServletRequest("GET", "/actuator/health");
            poisoned.addHeader(GatewayHeaders.HDR_AUTH_USER_ID, "forged-user");
            MockHttpServletResponse response = new MockHttpServletResponse();
            // conditions
            // act
            filter.doFilter(poisoned, response, new MockFilterChain());
            // assert
            assertThat(response.getStatus()).isEqualTo(401);
            // verify
        }

    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.addHeader(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE, "assertion");
        return request;
    }

    private static RequestProvenanceClaims claims(String issuer) {
        return RequestProvenanceClaims.issue(
                "A".repeat(22),
                issuer,
                "user-service",
                Instant.parse("2026-09-03T12:00:00Z"),
                "GET",
                "/api/users/me",
                "A".repeat(43),
                "B".repeat(43));
    }

}

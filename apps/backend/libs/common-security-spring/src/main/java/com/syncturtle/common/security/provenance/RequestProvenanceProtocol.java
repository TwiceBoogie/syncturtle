package com.syncturtle.common.security.provenance;

import java.time.Duration;
import java.util.Set;

public final class RequestProvenanceProtocol {

    public static final int VERSION = 1;
    public static final String ALGORITHM = "Ed25519";
    public static final Duration ASSERTION_LIFETIME = Duration.ofSeconds(10);
    public static final Duration ALLOWED_CLOCK_SKEW = Duration.ofSeconds(5);
    public static final int MAX_HEADER_CHARACTERS = 4096;
    public static final int MAX_PAYLOAD_BYTES = 2048;
    public static final int MAX_RAW_PATH_CHARACTERS = 2048;
    public static final int MAX_RAW_QUERY_CHARACTERS = 4096;
    public static final int ED25519_SIGNATURE_BYTES = 64;

    public static final Set<String> EXPECTED_FIELDS = Set.of(
            "v", "alg", "kid", "iss", "aud", "iat", "exp", "m", "p", "q", "h");

    private RequestProvenanceProtocol() {
    }

}

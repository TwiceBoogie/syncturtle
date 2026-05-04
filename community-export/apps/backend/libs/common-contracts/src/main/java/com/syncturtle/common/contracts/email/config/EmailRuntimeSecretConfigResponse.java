package com.syncturtle.common.contracts.email.config;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EmailRuntimeSecretConfigResponse {
    boolean enabled;
    String host;
    Integer port;
    String username;
    String password;
    String from;
    boolean useTls;
    boolean useSsl;
    long version;
}

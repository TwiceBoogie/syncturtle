package com.syncturtle.common.core.dto.response;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EmailRuntimeConfigResponse {
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

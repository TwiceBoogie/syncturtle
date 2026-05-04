package com.syncturtle.common.contracts.email.config;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SmtpRuntimeConfig {
    String host;
    String user;
    String password;
    int port;
    boolean useTls;
    boolean useSsl;
    String from;
}

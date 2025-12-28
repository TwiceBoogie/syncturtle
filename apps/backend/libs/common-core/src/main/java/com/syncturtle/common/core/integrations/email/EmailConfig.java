package com.syncturtle.common.core.integrations.email;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class EmailConfig {
    private final String host;
    private final String user;
    private final String password;
    private final int port;
    private final boolean useTls;
    private final boolean useSsl;
    private final String from;
}

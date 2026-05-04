package com.syncturtle.services.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRuntimeConfig {
    private boolean enabled;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String from;
    private boolean useTls;
    private boolean useSsl;
    private long version;

    public boolean isComplete() {
        return enabled
                && host != null && !host.isBlank()
                && port != null
                && from != null && !from.isBlank();
    }

    public boolean requiresAuthentication() {
        return username != null && !username.isBlank();
    }
}

package com.syncturtle.platform.services.instance.services.integrations.email;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.core.integrations.email.EmailConfig;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class EmailConfigFactory {

    private final InstanceConfigurationResolver resolver;

    public EmailConfig build() {
        Map<InstanceConfigurationKey, String> m = resolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST_USER, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST_PASSWORD, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_PORT, "587"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_USE_TLS, "1"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_USE_SSL, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_FROM,
                        "Team Syncturtle <team@mailer.syncturtle.com>")));

        String host = m.get(InstanceConfigurationKey.EMAIL_HOST);
        String user = m.get(InstanceConfigurationKey.EMAIL_HOST_USER);
        String password = m.get(InstanceConfigurationKey.EMAIL_HOST_PASSWORD);

        int port = InstanceConfigurationResolver.parseIntOr(m.get(InstanceConfigurationKey.EMAIL_PORT), 587);
        boolean tls = "1".equals(m.get(InstanceConfigurationKey.EMAIL_USE_TLS));
        boolean ssl = "1".equals(m.get(InstanceConfigurationKey.EMAIL_USE_SSL));

        String from = m.get(InstanceConfigurationKey.EMAIL_FROM);
        if (!StringUtils.hasText(from)) {
            from = "Team Syncturtle <team@mailer.syncturtle.com>";
        }

        return new EmailConfig(host, user, password, port, tls, ssl, from);
    }
}

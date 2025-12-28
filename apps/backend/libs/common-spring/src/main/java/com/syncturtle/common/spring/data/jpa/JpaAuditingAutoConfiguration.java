package com.syncturtle.common.spring.data.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.syncturtle.common.core.audit.AuditingConstants;
import com.syncturtle.common.web.context.RequestUserContext;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = {
        "jakarta.persistence.EntityManager",
        "org.springframework.data.jpa.repository.config.EnableJpaAuditing"
})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingAutoConfiguration {

    /**
     * Auditor resolution:
     * - If RequestUserContext is present and has a user -> use it
     * - Else fallback to SYSTEM_USER_ID for audit columns
     * 
     * @param ctxProvider
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    AuditorAware<UUID> auditorAware(ObjectProvider<RequestUserContext> ctxProvider) {
        return () -> {
            RequestUserContext ctx = ctxProvider.getIfAvailable();
            UUID fromRequest = (ctx != null) ? ctx.getUserId() : null;
            return Optional.ofNullable(fromRequest)
                    .or(() -> Optional.of(AuditingConstants.SYSTEM_USER_ID));
        };
    }

}

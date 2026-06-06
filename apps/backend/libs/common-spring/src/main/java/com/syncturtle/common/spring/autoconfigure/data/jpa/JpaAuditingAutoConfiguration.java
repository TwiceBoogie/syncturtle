package com.syncturtle.common.spring.autoconfigure.data.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.syncturtle.common.core.actor.SystemActors;
import com.syncturtle.common.spring.autoconfigure.web.GatewayContextAutoConfiguration;
import com.syncturtle.common.web.context.RequestUserContext;

@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
}, after = GatewayContextAutoConfiguration.class)
@ConditionalOnClass(name = {
        "jakarta.persistence.EntityManager",
        "org.springframework.data.domain.AuditorAware",
        "org.springframework.data.jpa.repository.config.EnableJpaAuditing"
})
@ConditionalOnBean(name = "entityManagerFactory")
@ConditionalOnProperty(prefix = "app.data.jpa.auditing", name = "enabled", havingValue = "true", matchIfMissing = true)
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
    @Bean(name = "auditorAware")
    @ConditionalOnMissingBean(name = "auditorAware")
    AuditorAware<UUID> auditorAware(ObjectProvider<RequestUserContext> ctxProvider) {
        return () -> {
            RequestUserContext ctx = ctxProvider.getIfAvailable();
            UUID fromRequest = (ctx != null) ? ctx.getUserId() : null;
            return Optional.ofNullable(fromRequest)
                    .or(() -> Optional.of(SystemActors.SYSTEM_USER_ID));
        };
    }

}

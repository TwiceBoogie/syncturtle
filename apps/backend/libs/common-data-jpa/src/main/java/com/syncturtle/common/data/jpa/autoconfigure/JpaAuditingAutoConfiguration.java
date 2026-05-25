package com.syncturtle.common.data.jpa.autoconfigure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.syncturtle.common.core.actor.CurrentActorProvider;
import com.syncturtle.common.core.actor.SystemActors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@AutoConfiguration(after = {
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
})
@ConditionalOnClass({
        EntityManager.class,
        AuditorAware.class,
        EnableJpaAuditing.class
})
@ConditionalOnBean(EntityManagerFactory.class)
@ConditionalOnProperty(prefix = "app.data.jpa.auditing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JpaAuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AuditorAware<UUID> auditorAware(ObjectProvider<CurrentActorProvider> actorProvider) {
        return () -> {
            CurrentActorProvider provider = actorProvider.getIfAvailable();

            if (provider != null) {
                Optional<UUID> actorId = provider.currectActorId();
                if (actorId.isPresent()) {
                    return actorId;
                }
            }

            return Optional.of(SystemActors.SYSTEM_USER_ID);
        };
    }

}

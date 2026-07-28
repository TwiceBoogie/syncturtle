package com.syncturtle.common.data.jpa.autoconfigure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.syncturtle.common.core.actor.CurrentActorProvider;
import com.syncturtle.common.core.actor.SystemActors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@AutoConfiguration(after = {
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class
})
@ConditionalOnClass({
        EntityManager.class,
        EntityManagerFactory.class,
        AuditorAware.class,
        EnableJpaAuditing.class
})
@ConditionalOnBean(EntityManagerFactory.class)
@ConditionalOnBooleanProperty(prefix = "app.data.jpa.auditing", name = "enabled", matchIfMissing = true)
@EnableJpaAuditing
public class JpaAuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    AuditorAware<UUID> auditorAware(
            ObjectProvider<CurrentActorProvider> actorProvider) {
        return () -> {
            CurrentActorProvider provider = actorProvider.getIfAvailable();

            if (provider != null) {
                Optional<UUID> actorId = provider.currentActorId();

                if (actorId.isPresent()) {
                    return actorId;
                }
            }

            return Optional.of(SystemActors.SYSTEM_USER_ID);
        };
    }
}
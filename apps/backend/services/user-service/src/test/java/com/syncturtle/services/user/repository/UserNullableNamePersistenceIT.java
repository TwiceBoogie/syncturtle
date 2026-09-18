package com.syncturtle.services.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.syncturtle.common.core.actor.PrincipalType;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.model.param.UserCreateParam;
import com.syncturtle.testing.annotation.JpaIntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

import jakarta.persistence.EntityManager;

@JpaIntegrationTest
@UsePostgresDb("user_service_nullable_name_it")
@EnableJpaAuditing
@DisplayName("User nullable-name persistence")
class UserNullableNamePersistenceIT {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-01T12:00:00Z"),
            ZoneOffset.UTC);

    @Autowired
    UserRepository userRepository;
    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("persists a credential-created human user without names")
    void persistsCredentialCreatedHumanUserWithoutNames() {
        // arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserCreateParam param = UserCreateParam.builder()
                .username("storm-" + suffix)
                .email("storm-" + suffix + "@example.com")
                .passwordHash("$2a$12$nullable-name-persistence-test")
                .passwordAutoset(false)
                .principalType(PrincipalType.HUMAN)
                .initialLoginIp("192.0.2.1")
                .initialLoginMedium("email")
                .initialLoginUserAgent("JUnit-test")
                .build();

        // act
        User saved = userRepository.saveAndFlush(User.create(param, CLOCK));
        UUID userId = saved.getId();
        entityManager.clear();
        User reloaded = userRepository.findById(userId).orElseThrow();

        // assert
        assertThat(reloaded.getPrincipalType()).isEqualTo(PrincipalType.HUMAN);
        assertThat(reloaded.getFirstName()).isNull();
        assertThat(reloaded.getLastName()).isNull();
    }
}

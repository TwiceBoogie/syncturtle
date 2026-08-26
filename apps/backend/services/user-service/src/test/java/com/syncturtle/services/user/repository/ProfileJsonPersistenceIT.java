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
import com.syncturtle.services.user.model.Profile;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.model.param.UserCreateParam;
import com.syncturtle.services.user.model.support.JsonDefaults;
import com.syncturtle.testing.annotation.JpaIntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

import jakarta.persistence.EntityManager;

@JpaIntegrationTest
@UsePostgresDb("user_service_profile_json_it")
@EnableJpaAuditing
@DisplayName("Profile JSON persistence")
class ProfileJsonPersistenceIT {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T12:00:00Z"),
            ZoneOffset.UTC);

    @Autowired
    UserRepository userRepository;
    @Autowired
    ProfileRepository profileRepository;
    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("persists and reloads Jackson 3 JSON nodes")
    void persistsAndReloadsJackson3JsonNodes() {
        // arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserCreateParam userParam = UserCreateParam.builder()
                .username("luna-" + suffix)
                .email("luna-" + suffix + "@marvel.com")
                .firstName("Luna")
                .lastName("Snow")
                .passwordHash("$2a$12$profile-json-persistence-test")
                .passwordAutoset(false)
                .principalType(PrincipalType.HUMAN)
                .initialLoginIp("192.0.2.1")
                .initialLoginMedium("PASSWORD")
                .initialLoginUserAgent("JUnit-test")
                .build();
        User user = userRepository.saveAndFlush(User.create(userParam, CLOCK));
        Profile profile = Profile.create(user, CLOCK, "Marvel Inc");

        // act
        Profile saved = profileRepository.saveAndFlush(profile);
        UUID profileId = saved.getId();
        entityManager.clear();
        Profile reloaded = profileRepository.findById(profileId).orElseThrow();

        // assert
        assertThat(reloaded.getTheme()).isEqualTo(JsonDefaults.emptyObject());
        assertThat(reloaded.getOnboardingStep()).isEqualTo(JsonDefaults.profileOnboarding());
        assertThat(reloaded.getBillingAddress()).isEqualTo(JsonDefaults.emptyObject());
    }

}

package com.syncturtle.services.instance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.service.InstanceAuthorizationService;
import com.syncturtle.services.instance.service.impl.InstanceAuthorizationServiceImpl;
import com.syncturtle.services.instance.support.clock.TestClocks;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;
import com.syncturtle.services.instance.type.InstanceAdminRole;
import com.syncturtle.testing.annotation.JpaIntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

import jakarta.persistence.EntityManager;

@EnableJpaAuditing
@JpaIntegrationTest
@UsePostgresDb("instance_admin_authorization_it")
@Import(InstanceAuthorizationServiceImpl.class)
@DisplayName("Instance administrator authorization")
class InstanceAdminAuthorizationIT {

    @Autowired
    InstanceRepository instances;

    @Autowired
    InstanceAdminRepository admins;

    @Autowired
    InstanceAuthorizationService authorization;

    @Autowired
    EntityManager entityManager;

    @Nested
    @DisplayName("hasCurrentInstanceRoleAtLeast(UUID, InstanceAdminRole)")
    class HasCurrentInstanceRoleAtLeast {

        @Test
        @DisplayName("accepts the active owner and denies the same elevated identity after removal")
        void deniesRemovedOwnerDespitePreviouslyElevatedIdentity() {
            // arrange
            UUID userId = UUID.randomUUID();
            Instance instance = instances.saveAndFlush(InstanceFixtures.activeInstance());
            InstanceAdmin admin = admins.saveAndFlush(InstanceAdmin.createInitialOwner(userId, instance));
            entityManager.clear();

            // act + assert: an active owner has current authority
            assertThat(authorization.hasCurrentInstanceRoleAtLeast(userId, InstanceAdminRole.ADMIN)).isTrue();
            assertThat(admins.findByInstance_IdAndUserIdAndDeletedAtIsNull(instance.getId(), userId)).isPresent();

            // arrange: the same identity still exists, but its administrator row is soft deleted
            InstanceAdmin stored = admins.findById(admin.getId()).orElseThrow();
            stored.revoke(TestClocks.fixedUtc());
            admins.flush();
            entityManager.clear();

            // act
            boolean allowed = authorization.hasCurrentInstanceRoleAtLeast(userId, InstanceAdminRole.ADMIN);

            // assert
            assertThat(admins.findActiveRoleByInstanceIdAndUserId(instance.getId(), userId)).isEmpty();
            assertThat(admins.findByInstance_IdAndUserIdAndDeletedAtIsNull(instance.getId(), userId)).isEmpty();
            assertThat(allowed).isFalse();
            assertThat(authorization.hasCurrentInstanceRoleAtLeast(userId, InstanceAdminRole.USER)).isFalse();
        }

        @Test
        @DisplayName("preserves the current role threshold and instance ownership")
        void preservesCurrentRoleAndInstanceScope() {
            // arrange
            UUID userId = UUID.randomUUID();
            Instance instance = instances.saveAndFlush(InstanceFixtures.activeInstance());
            InstanceAdmin admin = InstanceAdmin.createAdmin(userId, instance);
            admin.changeRole(InstanceAdminRole.USER);
            admins.saveAndFlush(admin);
            entityManager.clear();

            // act
            boolean userAllowed = authorization.hasCurrentInstanceRoleAtLeast(userId, InstanceAdminRole.USER);
            boolean adminAllowed = authorization.hasCurrentInstanceRoleAtLeast(userId, InstanceAdminRole.ADMIN);

            // assert
            assertThat(userAllowed).isTrue();
            assertThat(adminAllowed).isFalse();
            assertThat(admins.findActiveRoleByInstanceIdAndUserId(UUID.randomUUID(), userId)).isEmpty();
        }
    }
}

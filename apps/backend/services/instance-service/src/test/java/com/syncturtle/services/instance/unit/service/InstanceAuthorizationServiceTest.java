package com.syncturtle.services.instance.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.projection.InstanceIdProjection;
import com.syncturtle.services.instance.service.InstanceAuthorizationService;
import com.syncturtle.services.instance.service.impl.InstanceAuthorizationServiceImpl;
import com.syncturtle.services.instance.type.InstanceAdminRole;

@DisplayName("InstanceAuthorizationService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class InstanceAuthorizationServiceTest {

    @Mock
    InstanceRepository instanceRepository;
    @Mock
    InstanceAdminRepository instanceAdminRepository;

    InstanceAuthorizationService service;

    @BeforeEach
    void setup() {
        service = new InstanceAuthorizationServiceImpl(instanceRepository, instanceAdminRepository);
    }

    @Nested
    @DisplayName("hasCurrentInstanceRoleAtLeast(UUID, InstanceAdminRole)")
    class hasCurrentInstanceRoleAtLeast {

        @Test
        @DisplayName("rejects null user id before hitting repositories")
        void rejectsNullUserIdBeforeHittingRepositories() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.hasCurrentInstanceRoleAtLeast(null, InstanceAdminRole.ADMIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("userId is required");
            // verify
            verifyNoInteractions(instanceRepository, instanceAdminRepository);
        }

        @Test
        @DisplayName("rejects null required role before hitting repositories")
        void rejectsNullRequiredRoleBeforeHittingRepositories() {
            // arrange
            UUID userId = UUID.randomUUID();
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.hasCurrentInstanceRoleAtLeast(userId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("requiredRole is required");
            // verify
            verifyNoInteractions(instanceRepository, instanceAdminRepository);
        }

        @Test
        @DisplayName("returns false when no current instance exists")
        void returnsFalseWhenNoCurrentInstanceExists() {
            // arrange
            UUID userId = UUID.randomUUID();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class))
                    .thenReturn(Optional.empty());
            // act
            boolean actual = service.hasCurrentInstanceRoleAtLeast(userId, InstanceAdminRole.ADMIN);
            // assert
            assertThat(actual).isFalse();
            // verify
            verify(instanceRepository).findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class);
            verifyNoInteractions(instanceAdminRepository);
        }

        @Test
        @DisplayName("returns false when user has no active role in current instance")
        void returnsFalseWhenUserHasNoActiveRoleInCurrentInstance() {
            // arrange
            UUID instanceId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            InstanceIdProjection instanceIdProjection = mock(InstanceIdProjection.class);
            // conditions
            when(instanceIdProjection.getId()).thenReturn(instanceId);
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class))
                    .thenReturn(Optional.of(instanceIdProjection));
            when(instanceAdminRepository.findActiveRoleByInstanceIdAndUserId(instanceId, userId))
                    .thenReturn(Optional.empty());
            // act
            boolean actual = service.hasCurrentInstanceRoleAtLeast(userId, InstanceAdminRole.ADMIN);
            // assert
            assertThat(actual).isFalse();
            // verify
            verify(instanceAdminRepository).findActiveRoleByInstanceIdAndUserId(instanceId, userId);
        }

        @Test
        @DisplayName("returns true when actual role is at least the required role")
        void returnsTrueWhenActualRoleIsAtLeastTheRequiredRole() {
            // arrange
            UUID instanceId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            InstanceIdProjection instanceIdProjection = mock(InstanceIdProjection.class);
            // conditions
            when(instanceIdProjection.getId()).thenReturn(instanceId);
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class))
                    .thenReturn(Optional.of(instanceIdProjection));
            when(instanceAdminRepository.findActiveRoleByInstanceIdAndUserId(instanceId, userId))
                    .thenReturn(Optional.of(InstanceAdminRole.ADMIN));
            // act
            boolean actual = service.hasCurrentInstanceRoleAtLeast(userId, InstanceAdminRole.ADMIN);
            // assert
            assertThat(actual).isTrue();
            // verify
            verify(instanceAdminRepository).findActiveRoleByInstanceIdAndUserId(instanceId, userId);
        }

    }

}
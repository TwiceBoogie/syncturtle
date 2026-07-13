package com.syncturtle.services.instance.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationResponse;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.service.InstanceAdminInternalService;
import com.syncturtle.services.instance.service.impl.InstanceAdminInternalServiceImpl;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;
import com.syncturtle.services.instance.support.fixture.RequestFixtures;

@DisplayName("InstanceAdminInternalService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class InstanceAdminInternalServiceTest {

    @Mock
    InstanceAdminRepository repository;

    InstanceAdminInternalService service;

    @BeforeEach
    void setup() {
        service = new InstanceAdminInternalServiceImpl(repository);
    }

    @Nested
    @DisplayName("resolveInstanceAuthz(ResolveInstanceAuthorizationRequest)")
    class ResolveInstanceAuthzTests {

        @Test
        @DisplayName("rejects null request before hitting repository")
        void rejectsNullRequestBeforeHittingRepository() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.resolveInstanceAuthz(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("authorization request is required");
            // verify
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("rejects request without instance id before hitting repository")
        void rejectsRequestWithoutInstanceIdBeforeHittingRepository() {
            // arrange
            ResolveInstanceAuthorizationRequest request = RequestFixtures.noInstanceId();
            // act + assert
            assertThatThrownBy(() -> service.resolveInstanceAuthz(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("instanceId is required");
            // verify
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("rejects request without user id before hitting repository")
        void rejectsRequestWithoutUserIdBeforeHittingRepository() {
            // arrange
            ResolveInstanceAuthorizationRequest request = RequestFixtures.noUserId();
            // act + assert
            assertThatThrownBy(() -> service.resolveInstanceAuthz(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("userId is required");
            // verify
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("returns not admin response when admin row does not exist")
        void returnsNotAdminResponseWhenAdminRowDoesNotExist() {
            // arrange
            ResolveInstanceAuthorizationRequest request = RequestFixtures.validResolveInstanceAuthorizationRequest();
            // conditions
            when(repository.findByInstance_IdAndUserIdAndDeletedAtIsNull(request.getInstanceId(), request.getUserId()))
                    .thenReturn(Optional.empty());
            // act
            ResolveInstanceAuthorizationResponse actual = service.resolveInstanceAuthz(request);
            // assert
            assertThat(actual.getInstanceId()).isEqualTo(request.getInstanceId());
            assertThat(actual.getUserId()).isEqualTo(request.getUserId());
            assertThat(actual.isInstanceAdmin()).isFalse();
            assertThat(actual.getAdminSessionVersion()).isNull();
            // verify
            verify(repository).findByInstance_IdAndUserIdAndDeletedAtIsNull(request.getInstanceId(),
                    request.getUserId());
        }

        @Test
        @DisplayName("returns admin response when active admin row exists")
        void returnsAdminResponseWhenActiveAdminRowExists() {
            // arrange
            UUID userId = UUID.randomUUID();
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            InstanceAdmin admin = InstanceAdmin.createAdmin(userId, instance);

            ResolveInstanceAuthorizationRequest request = new ResolveInstanceAuthorizationRequest(userId,
                    instance.getId());
            // conditions
            when(repository.findByInstance_IdAndUserIdAndDeletedAtIsNull(instance.getId(), userId))
                    .thenReturn(Optional.of(admin));
            // act
            ResolveInstanceAuthorizationResponse actual = service.resolveInstanceAuthz(request);
            // assert
            assertThat(actual.getInstanceId()).isEqualTo(instance.getId());
            assertThat(actual.getUserId()).isEqualTo(userId);
            assertThat(actual.isInstanceAdmin()).isTrue();
            assertThat(actual.getAdminSessionVersion()).isEqualTo(admin.getSessionVersion());
            // verify
            verify(repository).findByInstance_IdAndUserIdAndDeletedAtIsNull(instance.getId(), userId);
        }

    }

}

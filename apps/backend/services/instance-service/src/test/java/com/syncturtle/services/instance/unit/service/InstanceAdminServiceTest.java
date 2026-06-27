package com.syncturtle.services.instance.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.services.instance.dto.response.InstanceAdminMeResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminSessionResponse;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.UserRepository;
import com.syncturtle.services.instance.repository.projection.AdminUserDetailLiteProjection;
import com.syncturtle.services.instance.repository.projection.AdminUserDetailsProjection;
import com.syncturtle.services.instance.repository.projection.InstanceAdminProjection;
import com.syncturtle.services.instance.service.InstanceAdminService;
import com.syncturtle.services.instance.service.impl.InstanceAdminServiceImpl;
import com.syncturtle.services.instance.service.mapper.InstanceAdminApiMapper;
import com.syncturtle.services.instance.support.clock.TestClocks;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;
import com.syncturtle.services.instance.type.InstanceAdminRole;

@DisplayName("InstanceAdminService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class InstanceAdminServiceTest {

    @Mock
    InstanceRepository instanceRepository;
    @Mock
    InstanceAdminRepository instanceAdminRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    InstanceAdminApiMapper mapper;

    @Mock
    InstanceAdminProjection adminProjectionOne;
    @Mock
    InstanceAdminProjection adminProjectionTwo;
    @Mock
    AdminUserDetailLiteProjection userProjectionOne;
    @Mock
    AdminUserDetailLiteProjection userProjectionTwo;
    @Mock
    AdminUserDetailsProjection adminDetailsProjection;

    Clock clock;
    InstanceAdminService service;

    @BeforeEach
    void setup() {
        clock = TestClocks.fixedUtc();
        service = new InstanceAdminServiceImpl(instanceRepository, instanceAdminRepository, userRepository, mapper,
                clock);
    }

    @Nested
    @DisplayName("getAdmins()")
    class getAdminsTests {

        @Test
        @DisplayName("throws FORBIDDEN when instance is not configured")
        void throwsForbiddenWhenInstanceIsNotConfigured() {
            // arrange
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.empty());
            // act
            ResponseStatusException exception = catchThrowableOfType(ResponseStatusException.class,
                    () -> service.getAdmins());
            // assert
            assertThat(exception).isNotNull();
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(exception.getReason()).isEqualTo("Instance is not yet registered.");
            // verify
            verifyNoInteractions(instanceAdminRepository, userRepository, mapper);
        }

        @Test
        @DisplayName("returns empty list when instance has no admins")
        void returnsEmptyListWhenInstanceHasNoAdmins() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.findByInstance_IdAndDeletedAtIsNull(instance.getId())).thenReturn(List.of());
            // act
            List<InstanceAdminResponse> actual = service.getAdmins();
            // assert
            assertThat(actual).isEmpty();
            // verify
            verifyNoInteractions(userRepository, mapper);
        }

        @Test
        @DisplayName("loads admin projection, batches users, and maps responses")
        void loadsAdminProjectionsBatchesUsersAndMapsResponses() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID userIdOne = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID userIdTwo = UUID.fromString("22222222-2222-2222-2222-222222222222");
            InstanceAdminResponse responseOne = InstanceAdminResponse.builder().build();
            InstanceAdminResponse responseTwo = InstanceAdminResponse.builder().build();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.findByInstance_IdAndDeletedAtIsNull(instance.getId()))
                    .thenReturn(List.of(adminProjectionOne, adminProjectionTwo));
            when(adminProjectionOne.getUserId()).thenReturn(userIdOne);
            when(adminProjectionTwo.getUserId()).thenReturn(userIdTwo);
            when(userRepository.findByIdInAndDeletedAtIsNull(List.of(userIdOne, userIdTwo)))
                    .thenReturn(List.of(userProjectionOne, userProjectionTwo));
            when(userProjectionOne.getId()).thenReturn(userIdOne);
            when(userProjectionTwo.getId()).thenReturn(userIdTwo);
            when(mapper.toResponse(same(adminProjectionOne), same(userProjectionOne))).thenReturn(responseOne);
            when(mapper.toResponse(same(adminProjectionTwo), same(userProjectionTwo))).thenReturn(responseTwo);
            // act
            List<InstanceAdminResponse> actual = service.getAdmins();
            // assert
            assertThat(actual).containsExactly(responseOne, responseTwo);
            // verify
            InOrder order = inOrder(instanceRepository, instanceAdminRepository, userRepository, mapper);
            order.verify(instanceRepository).findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class);
            order.verify(instanceAdminRepository).findByInstance_IdAndDeletedAtIsNull(instance.getId());
            order.verify(userRepository).findByIdInAndDeletedAtIsNull(List.of(userIdOne, userIdTwo));
            order.verify(mapper).toResponse(same(adminProjectionOne), same(userProjectionOne));
            order.verify(mapper).toResponse(same(adminProjectionTwo), same(userProjectionTwo));
        }

    }

    @Nested
    @DisplayName("createAdmin(String)")
    class createAdminTests {

        @Test
        @DisplayName("rejects blank email before hitting repositories")
        void rejectsBlankEmailBeforeHittingRepositories() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.createAdmin(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("email is required");
            // verify
            verifyNoInteractions(instanceRepository, instanceAdminRepository, userRepository, mapper);
        }

        @Test
        @DisplayName("throws NOT_FOUND when user does not exist")
        void throwsNotFoundWhenUserDoesNotExist() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(userRepository.findIdByEmailIgnoreCase("lunasnow@marvel.com")).thenReturn(Optional.empty());
            // act + assert
            assertThatThrownBy(() -> service.createAdmin(" LUNASNOW@MARVEL.COM"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> {
                        ResponseStatusException exception = (ResponseStatusException) error;
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(exception.getReason()).isEqualTo("User does not exist.");
                    });
            // verify
            verify(userRepository).findIdByEmailIgnoreCase("lunasnow@marvel.com");
            verify(instanceAdminRepository, never()).save(any(InstanceAdmin.class));
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("throws CONFLICT when user is already an instance admin")
        void throwsConflictWhenUserIsAlreadyAnInstanceAdmin() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID userId = UUID.randomUUID();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(userRepository.findIdByEmailIgnoreCase("lunasnow@marvel.com")).thenReturn(Optional.of(userId));
            when(instanceAdminRepository.existsByInstance_IdAndUserIdAndDeletedAtIsNull(instance.getId(), userId))
                    .thenReturn(true);
            // act + assert
            assertThatThrownBy(() -> service.createAdmin(" LUNASNOW@MARVEL.COM"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> {
                        ResponseStatusException exception = (ResponseStatusException) error;
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(exception.getReason()).isEqualTo("User is already an instance admin");
                    });
            // verify
            verify(instanceAdminRepository, never()).save(any(InstanceAdmin.class));
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("creates admin, reloads user projection, and maps response")
        void createsAdminReloadsUserProjectionAndMapsResponse() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID userId = UUID.randomUUID();
            InstanceAdminResponse expected = InstanceAdminResponse.builder().build();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(userRepository.findIdByEmailIgnoreCase("lunasnow@marvel.com")).thenReturn(Optional.of(userId));
            when(instanceAdminRepository.existsByInstance_IdAndUserIdAndDeletedAtIsNull(instance.getId(), userId))
                    .thenReturn(false);
            when(instanceAdminRepository.save(any(InstanceAdmin.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(userProjectionOne);
            when(mapper.toResponse(any(InstanceAdmin.class), same(userProjectionOne))).thenReturn(expected);
            // act
            InstanceAdminResponse actual = service.createAdmin(" LUNASNOW@MARVEL.COM");
            // assert
            assertThat(actual).isSameAs(expected);
            // verify
            verify(userRepository).findIdByEmailIgnoreCase("lunasnow@marvel.com");
            verify(instanceAdminRepository).save(any(InstanceAdmin.class));
            verify(userRepository).findByIdAndDeletedAtIsNull(userId);
            verify(mapper).toResponse(any(InstanceAdmin.class), same(userProjectionOne));
        }

    }

    @Nested
    @DisplayName("deleteAdmin(UUID, UUID)")
    class deleteAdminTests {

        @Test
        @DisplayName("rejects null current user id before hitting repositories")
        void rejectsNullCurrentUserIdBeforeHittingRepositories() {
            // arrange
            UUID instanceAdminId = UUID.randomUUID();
            // act + assert
            assertThatThrownBy(() -> service.deleteAdmin(null, instanceAdminId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currentUserId is required");
            // verify
            verifyNoInteractions(instanceRepository, instanceAdminRepository, userRepository, mapper);
        }

        @Test
        @DisplayName("rejects null instance admin id before hitting repositories")
        void rejectsNullInstanceAdminIdBeforeHittingRespositories() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            // act + assert
            assertThatThrownBy(() -> service.deleteAdmin(currentUserId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("instanceAdminId is required");
            // verify
            verifyNoInteractions(instanceRepository, instanceAdminRepository, userRepository, mapper);
        }

        @Test
        @DisplayName("throws NOT_FOUND when target admin does not exist")
        void throwsNotFoundWhenTargetAdminDoesNotExist() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID currentUserId = UUID.randomUUID();
            UUID instanceAdminId = UUID.randomUUID();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.findByIdAndDeletedAtIsNull(instanceAdminId)).thenReturn(Optional.empty());
            // act + assert
            assertThatThrownBy(() -> service.deleteAdmin(currentUserId, instanceAdminId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> {
                        ResponseStatusException exception = (ResponseStatusException) error;
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(exception.getReason()).isEqualTo("Instance admin does not exist.");
                    });
            // verify
        }

        @Test
        @DisplayName("throws CONFLICT when removing own admin access")
        void throwsConflictWhenRemovingOwnAdminAccess() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID currentUserId = UUID.randomUUID();
            UUID instanceAdminId = UUID.randomUUID();
            InstanceAdmin target = InstanceAdmin.createAdmin(currentUserId, instance);
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.findByIdAndDeletedAtIsNull(instanceAdminId)).thenReturn(Optional.of(target));
            // act + assert
            assertThatThrownBy(() -> service.deleteAdmin(currentUserId, instanceAdminId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> {
                        ResponseStatusException exception = (ResponseStatusException) error;
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(exception.getReason())
                                .isEqualTo("You cannot remove your own admin access from this endpoint");
                    });
            // verify
        }

        @Test
        @DisplayName("throws CONFLICT when removing last remaining admin")
        void throwsConflictWhenRemovingLastRemainingAdmin() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID currentUserId = UUID.randomUUID();
            UUID targetUserId = UUID.randomUUID();
            UUID instanceAdminId = UUID.randomUUID();
            InstanceAdmin target = InstanceAdmin.createAdmin(targetUserId, instance);
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.findByIdAndDeletedAtIsNull(instanceAdminId)).thenReturn(Optional.of(target));
            when(instanceAdminRepository.countByInstance_IdAndRoleAndDeletedAtIsNull(instance.getId(),
                    InstanceAdminRole.ADMIN)).thenReturn(1L);
            // act + assert
            assertThatThrownBy(() -> service.deleteAdmin(currentUserId, instanceAdminId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> {
                        ResponseStatusException exception = (ResponseStatusException) error;
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(exception.getReason()).isEqualTo("Cannot remove the last remaining instance admin.");
                    });
            assertThat(target.getDeletedAt()).isNull();
            // verify
        }

        @Test
        @DisplayName("revokes target admin when another active admin remains")
        void revokesTargetAdminWhenAnotherActiveAdminRemains() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID currentUserId = UUID.randomUUID();
            UUID targetUserId = UUID.randomUUID();
            UUID instanceAdminId = UUID.randomUUID();
            InstanceAdmin target = InstanceAdmin.createAdmin(targetUserId, instance);
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.findByIdAndDeletedAtIsNull(instanceAdminId)).thenReturn(Optional.of(target));
            when(instanceAdminRepository.countByInstance_IdAndRoleAndDeletedAtIsNull(instance.getId(),
                    InstanceAdminRole.ADMIN)).thenReturn(2L);
            // act
            service.deleteAdmin(currentUserId, instanceAdminId);
            // assert
            assertThat(target.getDeletedAt()).isEqualTo(TestClocks.NOW);
            // verify
            verify(instanceAdminRepository, never()).save(any(InstanceAdmin.class));
        }

    }

    @Nested
    @DisplayName("getCurrentAdmin(UUID)")
    class getCurrentAdminTests {

        @Test
        @DisplayName("rejects null current user id before hitting repositories")
        void rejectsNullCurrentUserIdBeforeHittingRepositories() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.getCurrentAdmin(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currentUserId is required");
            // verify
            verifyNoInteractions(instanceRepository, instanceAdminRepository, userRepository, mapper);
        }

        @Test
        @DisplayName("throws FORBIDDEN when current user is not instance admin")
        void throwsForbiddenWhenCurrentUserIsNotInstanceAdmin() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID currentUserId = UUID.randomUUID();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.findCurrentAdminDetails(instance.getId(), currentUserId))
                    .thenReturn(Optional.empty());
            // act + assert
            assertThatThrownBy(() -> service.getCurrentAdmin(currentUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> {
                        ResponseStatusException exception = (ResponseStatusException) error;
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(exception.getReason()).isEqualTo("Current user is not an instance admin.");
                    });
            // verify
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("maps current admin details")
        void mapsCurrentAdminDetails() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID currentUserId = UUID.randomUUID();
            InstanceAdminMeResponse expected = InstanceAdminMeResponse.builder().build();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.findCurrentAdminDetails(instance.getId(), currentUserId))
                    .thenReturn(Optional.of(adminDetailsProjection));
            when(mapper.toMeResponse(same(adminDetailsProjection))).thenReturn(expected);
            // act
            InstanceAdminMeResponse actual = service.getCurrentAdmin(currentUserId);
            // assert
            assertThat(actual).isEqualTo(expected);
            // verify
            verify(mapper).toMeResponse(same(adminDetailsProjection));
        }

    }

    @Nested
    @DisplayName("getSession(UUID)")
    class getSessionTests {

        @Test
        @DisplayName("returns anonymous when current user id is null")
        void returnsAnonymousWhenCurrentUserIdIsNull() {
            // arrange
            // conditions
            // act
            InstanceAdminSessionResponse actual = service.getSession(null);
            // assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(InstanceAdminSessionResponse.anonymous());
            // verify
            verifyNoInteractions(instanceRepository, instanceAdminRepository, userRepository, mapper);
        }

        @Test
        @DisplayName("returns anonymous when instance is not configured")
        void returnsAnonymousWhenInstanceIsNotConfigured() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.empty());
            // act
            InstanceAdminSessionResponse actual = service.getSession(currentUserId);
            // assert
            assertThat(actual).isNotNull();
            // verify
            verifyNoInteractions(userRepository, mapper);
        }

        @Test
        @DisplayName("maps session when current user is an instance admin")
        void mapsSessionWhenCurrentUserIsAnInstanceAdmin() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            UUID currentUserId = UUID.randomUUID();
            InstanceAdminSessionResponse expected = InstanceAdminSessionResponse.builder().build();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.findCurrentAdminDetails(instance.getId(), currentUserId))
                    .thenReturn(Optional.of(adminDetailsProjection));
            when(mapper.toSessionResponse(same(adminDetailsProjection))).thenReturn(expected);
            // act
            InstanceAdminSessionResponse actual = service.getSession(currentUserId);
            // assert
            assertThat(actual).isSameAs(expected);
            // verify
            verify(mapper).toSessionResponse(same(adminDetailsProjection));
        }

    }

}

package com.syncturtle.services.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.core.actor.PrincipalType;
import com.syncturtle.common.core.asset.AssetContentUrlFactory;
import com.syncturtle.services.user.dto.request.UserOnboardUpdateRequest;
import com.syncturtle.services.user.dto.request.UserProfileUpdateRequest;
import com.syncturtle.services.user.dto.response.UserMeProfileResponse;
import com.syncturtle.services.user.mapper.UserApiMapper;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.model.Profile;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.model.param.UserCreateParam;
import com.syncturtle.services.user.repository.ProfileRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.repository.WorkspaceMemberInviteLiteRepository;
import com.syncturtle.services.user.repository.WorkspaceMemberLiteRepository;
import com.syncturtle.services.user.service.UserService;
import com.syncturtle.services.user.service.collaborator.asset.UserFileAssetVerifier;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;

class UserPersonalFirstOnboardingTest {

    @Nested
    class Completion {
        @Test
        void occupationIsNotRequiredAndLegacyOccupationRemainsReadable() {
            // arrange
            UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
            User user = User.create(UserCreateParam.builder().username("solo-user").email("solo@example.test")
                    .passwordHash("test-hash-with-at-least-twenty-characters").principalType(PrincipalType.HUMAN)
                    .build(), Clock.systemUTC());
            Profile profile = Profile.create(user, Clock.systemUTC(), null);
            ProfileRepository profiles = mock(ProfileRepository.class);
            UserService service = new UserServiceImpl(mock(UserRepository.class), profiles,
                    mock(WorkspaceMemberLiteRepository.class), mock(WorkspaceMemberInviteLiteRepository.class),
                    mock(UserFileAssetVerifier.class), mock(UserEventFactory.class), mock(UserOutboxWriter.class),
                    new UserApiMapper(mock(AssetContentUrlFactory.class)));
            // conditions
            when(profiles.findByUser_Id(id, Profile.class)).thenReturn(Optional.of(profile));
            // act: new completion needs no occupation/use-case/workspace replica.
            service.updateUserOnboard(id, UserOnboardUpdateRequest.builder().isOnboarded(true).build());
            // assert
            assertThat(profile.isOnboarded()).isTrue();
            assertThat(profile.getRole()).isNull();
            // act: legacy occupation still reads, and an unrelated update never resets
            // completion.
            UserMeProfileResponse response = service.updateUserProfile(id,
                    UserProfileUpdateRequest.builder().role("Developer").build());
            assertThat(response.getRole()).isEqualTo("Developer");
            assertThat(response.isOnBoarded()).isTrue();
            service.updateUserOnboard(id, UserOnboardUpdateRequest.builder().isOnboarded(false).build());
            assertThat(profile.isOnboarded()).isTrue();
            verify(profiles, times(3)).findByUser_Id(id, Profile.class);
        }
    }

}

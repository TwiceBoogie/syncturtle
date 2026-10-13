package com.syncturtle.services.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.model.Profile;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.ProfileRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.UserAdministrationInternalService;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffStore;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionClientFingerprintFactory;

class AdminSignupProductSeparationTest {

    @Test
    void initialAdminGetsOrdinaryUnonboardedProfileAndNoImplicitDestination() {
        // arrange
        UserRepository users = mock(UserRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        UserEventFactory events = mock(UserEventFactory.class);
        UserOutboxWriter outbox = mock(UserOutboxWriter.class);
        AdminSessionHandoffStore handoffs = mock(AdminSessionHandoffStore.class);
        UserAdministrationInternalService service = new UserAdministrationInternalServiceImpl(users, profiles,
                handoffs, mock(RefreshSessionClientFingerprintFactory.class), events, outbox, passwords,
                Clock.systemUTC());
        // Long installation names must not enter Profile's smaller company field.
        AdminSignupRequest request = AdminSignupRequest.builder().firstName("Solo").email("solo@example.test")
                .companyName("i".repeat(100)).password("Orbit#Cedar7!Turtle93@Violet")
                .clientIp("192.0.2.1").userAgent("test-browser").build();
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        // conditions
        when(passwords.encode(request.getPassword())).thenReturn("test-hash-with-at-least-twenty-characters");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", id);
            return saved;
        });
        UserEvent event = mock(UserEvent.class);
        when(events.created(any(User.class))).thenReturn(event);
        // act
        var response = service.adminSignup(request);
        // assert product onboarding and identity are separate from administration.
        var captor = org.mockito.ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(captor.capture());
        Profile profile = captor.getValue();
        // assert
        assertThat(response.getUserId()).isEqualTo(id);
        assertThat(profile.isOnboarded()).isFalse();
        assertThat(profile.getRole()).isNull();
        assertThat(profile.getUseCase()).isNull();
        assertThat(profile.getCompanyName()).isEmpty();
        assertThat(profile.getLastWorkspaceId()).isNull();
        assertThat(profile.getOnboardingStep().get("profileComplete").asBoolean()).isFalse();
        assertThat(profile.getOnboardingStep().get("workspaceCreate").asBoolean()).isFalse();
        assertThat(profile.getOnboardingStep().get("workspaceJoin").asBoolean()).isFalse();
        assertThat(profile.getOnboardingStep().get("workspaceInvite").asBoolean()).isFalse();
        // verify
        verify(outbox).saveUserEvent(event);
        verifyNoInteractions(handoffs);
    }

}

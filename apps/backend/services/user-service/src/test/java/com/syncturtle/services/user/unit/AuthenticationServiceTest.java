package com.syncturtle.services.user.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.spring.web.url.PublicUrlBuilder;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.user.models.Instance;
import com.syncturtle.services.user.repositories.InstanceRepository;
import com.syncturtle.services.user.repositories.UserRepository;
import com.syncturtle.services.user.services.FeatureFlagService;
import com.syncturtle.services.user.services.impl.AuthenticationServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    RequestUserContext userContext;
    @Mock
    RequestClientContext clientContext;
    @Mock
    PublicUrlBuilder resolver;
    @Mock
    InstanceRepository instanceRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    FeatureFlagService featureFlagService;

    @InjectMocks
    AuthenticationServiceImpl service;

    @Nested
    class EmailCheckTests {

        @Test
        void whenInstanceMissing_throwAuthenticationException_withCorrectErrorCode_andStopEarly() {
            // arrange
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
            // act + assert
            assertThatThrownBy(() -> service.emailCheck("test@example.com"))
                    .isInstanceOf(AuthException.class)
                    .satisfies(exception -> {
                        AuthException authException = (AuthException) exception;
                        assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
                        assertThat(authException.getMessage())
                                .isEqualTo(AuthErrorCode.INSTANCE_NOT_CONFIGURED.getKey());

                        // verify errorMap shape
                        assertThat(authException.getErrorMap())
                                .containsEntry("error_code", AuthErrorCode.INSTANCE_NOT_CONFIGURED.getCode())
                                .containsEntry("error_message", AuthErrorCode.INSTANCE_NOT_CONFIGURED.getKey());
                        assertThat(authException.getPayload()).isEmpty();
                    });

            verify(instanceRepository).findFirstByOrderByCreatedAtAsc();
            verifyNoInteractions(featureFlagService, userRepository, resolver, userContext, clientContext);
            verifyNoMoreInteractions(instanceRepository);
        }

        @Test
        void whenInstanceNotSetupDone_throwAuthenticationException_andDontReadFlagsOrUsers() {
            // arrange
            Instance instance = mock(Instance.class);
            when(instance.isSetupDone()).thenReturn(false);
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            // act + assert
            assertThatExceptionOfType(AuthException.class)
                    .isThrownBy(() -> service.emailCheck("test@example.com"))
                    .extracting(AuthException::getErrorCode)
                    .isEqualTo(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
            // verify
            verify(instanceRepository).findFirstByOrderByCreatedAtAsc();
            verify(instance).isSetupDone();
            verifyNoInteractions(featureFlagService, userRepository);
        }

        @Test
        void whenFailing_captureThrowable_andAssertMultipleFields_softly() {
            // arrange
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
            // act (capture)
            Throwable t = catchThrowable(() -> service.emailCheck("test@example.com"));

            // assert
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(t).isInstanceOf(AuthException.class);

            AuthException authException = (AuthException) t;
            softly.assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
            softly.assertThat(authException.getErrorMap()).containsKeys("error_code", "error_message");
            softly.assertThat(authException.getPayload()).isEmpty();
            softly.assertAll();

            // verify
            verifyNoInteractions(featureFlagService, userRepository);
        }

    }

}

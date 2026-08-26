package com.syncturtle.services.instance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindingResult;

import com.syncturtle.common.contracts.auth.session.AdminSessionHandoffResponse;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.instance.controller.validation.AuthFormValidator;
import com.syncturtle.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.services.instance.dto.response.InstanceAdminAuthResponse;
import com.syncturtle.services.instance.service.InstanceAdminAuthenticationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstanceAdminAuthenticationController")
class InstanceAdminAuthenticationControllerTest {

    @Mock
    InstanceAdminAuthenticationService service;
    @Mock
    ServletAuthCookieWriter cookieWriter;
    @Mock
    AuthFormValidator validator;
    @Mock
    PublicUrlResolver hostResolver;

    @InjectMocks
    private InstanceAdminAuthenticationController controller;

    @Nested
    @DisplayName("instanceAdminSignup()")
    class InstanceAdminSignupTests {

        @Test
        @DisplayName("writes the opaque handoff cookie and redirects to exact completion")
        void writesOpaqueHandoffCookieAndRedirects() {
            // arrange
            InstanceAdminSignupForm form = mock(InstanceAdminSignupForm.class);
            BindingResult bindingResult = mock(BindingResult.class);
            MockHttpServletResponse servletResponse = new MockHttpServletResponse();
            Instant issued = Instant.parse("2026-08-19T12:00:00Z");
            AdminSessionHandoffResponse handoff = AdminSessionHandoffResponse.builder()
                    .completionCode("receipt.secret")
                    .issuedAt(issued)
                    .expiresAt(issued.plusSeconds(30))
                    .build();
            // conditions
            when(validator.validate(any(), any(), any())).thenReturn(Optional.empty());
            when(service.instanceAdminSignup(any(), any())).thenReturn(InstanceAdminAuthResponse.builder()
                    .redirection("https://api.example.test/auth/admin/session")
                    .handoff(handoff)
                    .build());
            // act
            ResponseEntity<Void> response = controller.instanceAdminSignup(form, bindingResult, "a".repeat(64),
                    servletResponse);
            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(303);
            assertThat(response.getHeaders().getLocation())
                    .hasToString("https://api.example.test/auth/admin/session");
            verify(cookieWriter).setAdminSessionHandoffCookie(
                    servletResponse,
                    "receipt.secret",
                    Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("validation failure redirects without service call or handoff cookie")
        void validationFailureDoesNotCallServiceOrWriteHandoff() {
            // arrange
            InstanceAdminSignupForm form = mock(InstanceAdminSignupForm.class);
            BindingResult bindingResult = mock(BindingResult.class);
            MockHttpServletResponse servletResponse = new MockHttpServletResponse();
            AuthException failure = AuthException.of(AuthErrorCode.INVALID_ADMIN_EMAIL);
            // conditions
            when(validator.validate(any(), any(), any())).thenReturn(Optional.of(failure));
            when(hostResolver.adminWithQuery("", failure.getErrorMap()))
                    .thenReturn("https://admin.example.test?error=invalid");
            // act
            ResponseEntity<Void> response = controller.instanceAdminSignup(form, bindingResult, "a".repeat(64),
                    servletResponse);
            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(303);
            assertThat(response.getHeaders().getLocation())
                    .hasToString("https://admin.example.test?error=invalid");
            verifyNoInteractions(service, cookieWriter);
        }

    }

    @Nested
    @DisplayName("instanceAdminSignin()")
    class InstanceAdminSigninTests {

        @Test
        @DisplayName("writes only the bounded handoff cookie then redirects to completion")
        void writesOnlyTheBoundedHandoffCookieThenRedirectsToCompletion() {
            // arrange
            InstanceAdminSigninForm form = mock(InstanceAdminSigninForm.class);
            BindingResult bindingResult = mock(BindingResult.class);
            MockHttpServletResponse servletResponse = new MockHttpServletResponse();
            Instant issued = Instant.parse("2026-08-19T12:00:00Z");
            AdminSessionHandoffResponse handoff = AdminSessionHandoffResponse.builder()
                    .completionCode("receipt.secret")
                    .issuedAt(issued)
                    .expiresAt(issued.plusSeconds(30))
                    .build();
            // conditions
            when(validator.validate(any(), any(), any())).thenReturn(Optional.empty());
            when(service.instanceAdminSignin(any(), any())).thenReturn(InstanceAdminAuthResponse.builder()
                    .redirection("https://api.example.test/auth/admin/session").handoff(handoff).build());
            // act
            ResponseEntity<Void> response = controller.instanceAdminSignin(form, bindingResult, "a".repeat(64),
                    servletResponse);
            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(303);
            verify(cookieWriter).setAdminSessionHandoffCookie(servletResponse, "receipt.secret",
                    Duration.ofSeconds(30));
        }

    }

}

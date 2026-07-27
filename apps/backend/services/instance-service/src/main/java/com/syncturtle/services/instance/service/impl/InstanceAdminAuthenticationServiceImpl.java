package com.syncturtle.services.instance.service.impl;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.admin.AdminSigninResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSignupResponse;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.IssueInstanceAdminSessionRequest;
import com.syncturtle.common.contracts.auth.session.IssueSessionResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.instance.client.UserClient;
import com.syncturtle.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.services.instance.dto.response.InstanceAdminAuthResponse;
import com.syncturtle.services.instance.event.InstanceEventFactory;
import com.syncturtle.services.instance.messaging.outbox.InstanceOutboxWriter;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.model.param.InstanceSetupCompletionParam;
import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceAdminAuthenticationService;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceAdminAuthenticationServiceImpl implements InstanceAdminAuthenticationService {

    private final InstanceRepository instanceRepository;
    private final InstanceAdminRepository instanceAdminRepository;
    private final UserClient userClient;
    private final InstanceOutboxWriter outboxWriter;
    private final InstanceEventFactory eventFactory;
    private final PublicUrlResolver hostResolver;
    private final TransactionTemplate transactionTemplate;
    private final RequestClientContext requestClientContext;

    @Override
    public InstanceAdminAuthResponse instanceAdminSignup(InstanceAdminSignupForm form) {
        Assert.notNull(form, "signup form is required");

        try {
            InstancePreflight preflight = runSignupPreflight();

            String firstName = form.getFirstName().trim();
            String lastName = normalizeNullable(form.getLastName());
            String email = normalizeEmail(form.getEmail());
            String password = form.getPassword();
            String companyName = form.getCompanyName().trim();
            boolean telemetryEnabled = form.isTelemetryEnabled();

            AdminSignupResponse userResponse = userClient.adminSignupPost(
                    AdminSignupRequest.builder()
                            .firstName(firstName)
                            .lastName(lastName)
                            .email(email)
                            .companyName(companyName)
                            .telemetryEnabled(telemetryEnabled)
                            .password(password)
                            .clientIp(requestClientContext.getClientIp())
                            .userAgent(requestClientContext.getUserAgent())
                            .build());

            PendingAdminSession pending = Objects.requireNonNull(
                    transactionTemplate.execute(status -> completeInstanceAdminSignup(
                            userResponse.getUserId(),
                            preflight.getInstanceId(),
                            userResponse.getAuthVersion(),
                            companyName,
                            telemetryEnabled)),
                    "pending admin session is required");

            IssueSessionResponse session = issueInstanceAdminSession(pending);

            return InstanceAdminAuthResponse.builder()
                    .redirection(adminSuccessRedirect())
                    .session(session)
                    .build();
        } catch (AuthException exception) {
            return InstanceAdminAuthResponse.builder()
                    .redirection(hostResolver.adminWithQuery("", exception.getErrorMap()))
                    .session(null)
                    .build();
        }
    }

    @Override
    public InstanceAdminAuthResponse instanceAdminSignin(InstanceAdminSigninForm form) {
        Assert.notNull(form, "signin form is required");

        try {
            Instance instance = requireConfiguredInstance();

            String email = normalizeEmail(form.getEmail());
            String password = form.getPassword();

            AdminSigninResponse response = userClient.adminSigninPost(
                    AdminSigninRequest.builder()
                            .email(email)
                            .password(password)
                            .clientIp(requestClientContext.getClientIp())
                            .userAgent(requestClientContext.getUserAgent())
                            .build());

            PendingAdminSession pending = Objects.requireNonNull(
                    transactionTemplate.execute(status -> {
                        InstanceAdmin instanceAdmin = instanceAdminRepository
                                .findByInstance_IdAndUserId(instance.getId(), response.getUserId())
                                .orElseThrow(() -> AuthException
                                        .of(AuthErrorCode.ADMIN_AUTHENTICATION_FAILED)
                                        .with("email", email));

                        return PendingAdminSession.builder()
                                .userId(response.getUserId())
                                .instanceId(instance.getId())
                                .userAuthVersion(response.getAuthVersion())
                                .adminSessionVersion(instanceAdmin.getSessionVersion())
                                .build();
                    }),
                    "pending admin session is required");

            IssueSessionResponse session = issueInstanceAdminSession(pending);

            return InstanceAdminAuthResponse.builder()
                    .redirection(adminSuccessRedirect())
                    .session(session)
                    .build();
        } catch (AuthException exception) {
            return InstanceAdminAuthResponse.builder()
                    .redirection(hostResolver.adminWithQuery("", exception.getErrorMap()))
                    .session(null)
                    .build();
        }
    }

    private InstancePreflight runSignupPreflight() {
        return Objects.requireNonNull(
                transactionTemplate.execute(status -> {
                    Instance instance = requireConfiguredInstance();
                    validateNoAdminExists();

                    return new InstancePreflight(instance.getId());
                }),
                "instance preflight is required");
    }

    private PendingAdminSession completeInstanceAdminSignup(
            UUID userId,
            UUID instanceId,
            Long userAuthVersion,
            String companyName,
            boolean telemetryEnabled) {
        Assert.notNull(userId, "userId is required");
        Assert.notNull(instanceId, "instanceId is required");
        Assert.notNull(userAuthVersion, "userAuthVersion is required");
        Assert.hasText(companyName, "companyName is required");

        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> AuthException.of(AuthErrorCode.INSTANCE_NOT_CONFIGURED));

        validateNoAdminExists();

        InstanceAdmin instanceAdmin = InstanceAdmin.createInitialOwner(userId, instance);
        instanceAdmin = instanceAdminRepository.saveAndFlush(instanceAdmin);

        instance.completeSetup(InstanceSetupCompletionParam.builder()
                .companyName(companyName)
                .telemetryEnabled(telemetryEnabled)
                .build());
        instance = instanceRepository.saveAndFlush(instance);

        publishInstanceUpdate(instance);

        return PendingAdminSession.builder()
                .userId(userId)
                .instanceId(instance.getId())
                .userAuthVersion(userAuthVersion)
                .adminSessionVersion(instanceAdmin.getSessionVersion())
                .build();
    }

    private IssueSessionResponse issueInstanceAdminSession(PendingAdminSession pending) {
        Assert.notNull(pending, "pending session is required");

        return userClient.issueInstanceAdminSessionPost(
                IssueInstanceAdminSessionRequest.builder()
                        .userId(pending.getUserId())
                        .instanceId(pending.getInstanceId())
                        .userAuthVersion(pending.getUserAuthVersion())
                        .adminSessionVersion(pending.getAdminSessionVersion())
                        .clientIp(requestClientContext.getClientIp())
                        .userAgent(requestClientContext.getUserAgent())
                        .build());
    }

    private Instance requireConfiguredInstance() {
        return instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElseThrow(() -> AuthException.of(AuthErrorCode.INSTANCE_NOT_CONFIGURED));
    }

    private void validateNoAdminExists() {
        if (instanceAdminRepository.existsByDeletedAtIsNull()) {
            throw AuthException.of(AuthErrorCode.ADMIN_ALREADY_EXISTS);
        }
    }

    private String adminSuccessRedirect() {
        return hostResolver.admin("/general");
    }

    private void publishInstanceUpdate(Instance instance) {
        InstanceEvent event = eventFactory.updated(instance);
        outboxWriter.saveInstanceEvent(event);
    }

    private static String normalizeEmail(String email) {
        Assert.hasText(email, "email is required");
        return email.trim().toLowerCase();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Value
    private static class InstancePreflight {
        UUID instanceId;
    }

    @Value
    @Builder
    private static class PendingAdminSession {
        UUID userId;
        UUID instanceId;
        Long userAuthVersion;
        Long adminSessionVersion;
    }

}

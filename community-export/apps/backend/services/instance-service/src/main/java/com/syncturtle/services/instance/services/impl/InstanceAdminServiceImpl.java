package com.syncturtle.services.instance.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.contracts.instance.admin.AdminSigninResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSignupResponse;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.contracts.instance.event.InstanceEvent.Type;
import com.syncturtle.common.spring.web.url.PublicUrlBuilder;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.IssueInstanceAdminSessionRequest;
import com.syncturtle.common.contracts.auth.session.IssueSessionResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.services.instance.client.UserClient;
import com.syncturtle.services.instance.controllers.mappers.InstanceAdminApiMapper;
import com.syncturtle.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.services.instance.dto.response.InstanceAdminAuthResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.services.instance.models.Instance;
import com.syncturtle.services.instance.models.InstanceAdmin;
import com.syncturtle.services.instance.payload.InstanceEventToPublish;
import com.syncturtle.services.instance.repositories.InstanceAdminRepository;
import com.syncturtle.services.instance.repositories.InstanceRepository;
import com.syncturtle.services.instance.services.InstanceAdminService;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceAdminServiceImpl implements InstanceAdminService {

    // repositories
    private final InstanceRepository instanceRepository;
    private final InstanceAdminRepository instanceAdminRepository;
    // openFeign clients
    private final UserClient userClient;
    // messenger
    private final ApplicationEventPublisher events;
    // helpers
    private final PublicUrlBuilder hostResolver;
    private final TransactionTemplate transactionTemplate;
    // context
    private final RequestClientContext requestClientContext;
    // mapper
    private final InstanceAdminApiMapper mapper;

    @Override
    public InstanceAdminAuthResponse instanceAdminSignup(InstanceAdminSignupForm form) {
        try {
            InstancePreflight preflight = runSignupPreflight();

            String firstName = form.getFirstName();
            String lastName = form.getLastName();
            String email = form.getEmail().trim().toLowerCase();
            String password = form.getPassword();
            String companyName = form.getCompanyName();
            boolean telemetryEnabled = Boolean.parseBoolean(form.getTelemetryEnabled());

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

            PendingAdminSession pending = transactionTemplate.execute(status -> completeInstanceAdminSignup(
                    userResponse.getUserId(),
                    preflight.getInstanceId(),
                    userResponse.getAuthVersion(),
                    companyName,
                    telemetryEnabled));

            IssueSessionResponse issuedResponse = issueInstanceAdminSession(pending);

            return InstanceAdminAuthResponse.builder()
                    .redirection(adminSuccessRedirect())
                    .session(issuedResponse)
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
        try {
            Instance instance = requireConfiguredInstance();

            String email = form.getEmail().trim().toLowerCase();
            String password = form.getPassword();

            AdminSigninResponse response = userClient.adminSigninPost(
                    AdminSigninRequest.builder()
                            .email(email)
                            .password(password)
                            .clientIp(requestClientContext.getClientIp())
                            .userAgent(requestClientContext.getUserAgent())
                            .build());

            PendingAdminSession pending = transactionTemplate.execute(status -> {
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
            });

            IssueSessionResponse issuedResponse = issueInstanceAdminSession(pending);

            return InstanceAdminAuthResponse.builder()
                    .redirection(adminSuccessRedirect())
                    .session(issuedResponse)
                    .build();
        } catch (AuthException exception) {
            return InstanceAdminAuthResponse.builder()
                    .redirection(hostResolver.adminWithQuery("", exception.getErrorMap()))
                    .session(null)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceAdminResponse> getInstanceAdmins() {
        Instance instance = instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Instance is not registered yet"));

        return instanceAdminRepository.findByInstance_Id(instance.getId())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private InstancePreflight runSignupPreflight() {
        return transactionTemplate.execute(status -> {
            Instance instance = requireConfiguredInstance();
            validateNoAdminExists();

            return new InstancePreflight(instance.getId());
        });
    }

    private PendingAdminSession completeInstanceAdminSignup(
            UUID userId,
            UUID instanceId,
            Long userAuthVersion,
            String companyName,
            boolean telemetryEnabled) {
        Instance instance = instanceRepository.findById(instanceId).orElse(null);

        if (instance == null) {
            throw AuthException.of(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
        }

        validateNoAdminExists();

        InstanceAdmin instanceAdmin = InstanceAdmin.create(userId, instance);
        instanceAdmin.setSessionVersion(1L);
        instanceAdmin = instanceAdminRepository.saveAndFlush(instanceAdmin);

        instance.markSetupDone();
        instance.setInstanceName(companyName);
        instance.setTelemetryEnabled(telemetryEnabled);
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
        Instance instance = instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc()
                .orElse(null);

        if (instance == null) {
            throw AuthException.of(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
        }

        return instance;
    }

    private void validateNoAdminExists() {
        if (instanceAdminRepository.existsByIdIsNotNull()) {
            throw AuthException.of(AuthErrorCode.ADMIN_ALREADY_EXIST);
        }
    }

    private String adminSuccessRedirect() {
        return hostResolver.admin("/general");
    }

    private void publishInstanceUpdate(Instance instance) {
        InstanceEvent event = InstanceEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .type(Type.INSTANCE_UPDATED)
                .id(instance.getId())
                .setupDone(instance.isSetupDone())
                .edition(instance.getEdition())
                .version(instance.getVersion())
                .test(instance.isTest())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();

        events.publishEvent(new InstanceEventToPublish(event));
    }

    @Getter
    private static class InstancePreflight {
        private final UUID instanceId;

        private InstancePreflight(UUID instanceId) {
            this.instanceId = instanceId;
        }
    }

    @Getter
    @Builder
    private static class PendingAdminSession {
        private UUID userId;
        private UUID instanceId;
        private Long userAuthVersion;
        private Long adminSessionVersion;
    }

}

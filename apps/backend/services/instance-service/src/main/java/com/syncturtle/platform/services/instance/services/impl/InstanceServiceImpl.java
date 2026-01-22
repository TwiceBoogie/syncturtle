package com.syncturtle.platform.services.instance.services.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.core.exceptions.AuthenticationException;
import com.syncturtle.common.spring.web.url.HostUrlBuilder;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.common.web.dto.request.AdminSignupInternalRequest;
import com.syncturtle.common.web.dto.response.AdminSignupInternalResponse;
import com.syncturtle.platform.services.instance.client.UserClient;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSignupResult;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.platform.services.instance.models.Instance;
import com.syncturtle.platform.services.instance.models.InstanceAdmin;
import com.syncturtle.platform.services.instance.models.User;
import com.syncturtle.platform.services.instance.models.readmodel.InstanceInfoRow;
import com.syncturtle.platform.services.instance.repositories.InstanceAdminRepository;
import com.syncturtle.platform.services.instance.repositories.InstanceInfoAggregate;
import com.syncturtle.platform.services.instance.repositories.InstanceRepository;
import com.syncturtle.platform.services.instance.repositories.UserRepository;
import com.syncturtle.platform.services.instance.repositories.projections.InstanceAdminProjection;
import com.syncturtle.platform.services.instance.repositories.projections.InstanceOnlyIdProjection;
import com.syncturtle.platform.services.instance.services.InstanceService;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceServiceImpl implements InstanceService {

    // repositories
    private final InstanceRepository instanceRepository;
    private final InstanceAdminRepository instanceAdminRepository;
    private final UserRepository userRepository;
    // openFeign clients
    private final UserClient userClient;
    // helpers
    private final InstanceConfigurationResolver resolver;
    private final HostUrlBuilder hostResolver;
    private final Zxcvbn zxcvbn = new Zxcvbn();
    // context
    private final RequestUserContext userContext;

    @Override
    @Transactional(readOnly = true)
    public Optional<InstanceInfoAggregate> instanceInfoAndConfig() {
        // 1: grab instance info
        InstanceInfoRow instance = instanceRepository.findLatestInfoRow().orElse(null);

        if (instance == null) {
            return Optional.empty();
        }
        // 2: grab instance configurations
        Map<InstanceConfigurationKey, String> config = resolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_SIGNUP, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GOOGLE_ENABLED, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GITHUB_ENABLED, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GITHUB_APP_NAME, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GITLAB_ENABLED, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN, "1"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD, "1"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.POSTHOG_API_KEY, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.POSTHOG_HOST, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_INTERCOM_ENABLED, "1"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.INTERCOM_APP_ID, "")));

        // TODO: workspacesLiteRepository.count() >= 1 + usersLiteRepository.count()
        boolean workspacesExist = false;
        long userCount = userRepository.count();

        return Optional.of(new InstanceInfoAggregate(instance, config, workspacesExist, userCount));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getInstanceAdminUserMe() {
        return userRepository.findById(userContext.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceAdminProjection> getInstanceAdmins() {
        InstanceOnlyIdProjection instance = instanceRepository
                .findTopByOrderByCreatedAtDesc(InstanceOnlyIdProjection.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Instance is not registered yet"));

        return instanceAdminRepository.findAllByInstance_Id(instance.getInstanceId());
    }

    @Override
    @Transactional
    public InstanceAdminSignupResult instanceAdminSignup(InstanceAdminSignupForm form) {
        Instance instance = instanceRepository.findFirstByOrderByCreatedAtDesc().orElse(null);

        if (instance == null) {
            AuthenticationException exception = new AuthenticationException(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
            String url = hostResolver.buildAdminRedirectUriWithErrors(exception.getErrorMap());
            return new InstanceAdminSignupResult(null, url);
        }

        // check if instance already has admin registered
        if (instanceAdminRepository.existsByIdIsNotNull()) {
            AuthenticationException exception = new AuthenticationException(AuthErrorCode.ADMIN_ALREADY_EXIST);
            String url = hostResolver.buildAdminRedirectUriWithErrors(exception.getErrorMap());
            return new InstanceAdminSignupResult(null, url);
        }

        String firstName = form.getFirstName();
        String lastName = form.getLastName();
        String email = form.getEmail().trim().toLowerCase();
        String password = form.getPassword();
        String companyName = form.getCompanyName();
        boolean telemetryEnabled = Boolean.parseBoolean(form.getTelemetryEnabled());

        // user-service already checks for user admin and password strength
        if (userRepository.existsByEmailIgnoreCase(email)) {
            AuthenticationException exception = AuthenticationException.of(AuthErrorCode.ADMIN_USER_ALREADY_EXIST)
                    .with("firstName", firstName)
                    .with("lastName", lastName)
                    .with("email", email)
                    .with("companyName", companyName)
                    .with("isTelemetryEnabled", telemetryEnabled);
            String url = hostResolver.buildAdminRedirectUriWithErrors(exception.getErrorMap());
            return new InstanceAdminSignupResult(null, url);
        } else {
            Strength strength = zxcvbn.measure(password);
            int score = strength.getScore();
            if (score < 3) {
                AuthenticationException exception = AuthenticationException.of(AuthErrorCode.INVALID_ADMIN_PASSWORD)
                        .with("email", email)
                        .with("firstName", firstName)
                        .with("lastName", lastName)
                        .with("companyName", companyName)
                        .with("isTelemetryEnabled", telemetryEnabled);
                String url = hostResolver.buildAdminRedirectUriWithErrors(exception.getErrorMap());
                return new InstanceAdminSignupResult(null, url);
            }

            try {
                AdminSignupInternalResponse response = userClient
                        .adminSignupPost(new AdminSignupInternalRequest(firstName,
                                lastName, email, companyName, telemetryEnabled, password));

                InstanceAdmin instanceAdmin = instanceAdminRepository
                        .save(InstanceAdmin.create(response.getUserId(), instance));

                instance.setSetupDone(true);
                instance.setInstanceName(companyName);
                instance.setTelemetryEnabled(telemetryEnabled);
                instanceRepository.save(instance);

                return new InstanceAdminSignupResult(instanceAdmin.getUserId(), hostResolver.adminHost() + "/general");
            } catch (AuthenticationException exception) {
                return new InstanceAdminSignupResult(null,
                        hostResolver.buildAdminRedirectUriWithErrors(exception.getErrorMap()));
            }
        }
    }

}

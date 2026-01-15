package com.syncturtle.platform.services.instance.services.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.web.context.RequestUserContext;
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
    // instance configuration resolver
    private final InstanceConfigurationResolver resolver;
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

}

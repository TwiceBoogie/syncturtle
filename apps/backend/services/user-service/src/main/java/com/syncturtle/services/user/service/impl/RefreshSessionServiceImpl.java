package com.syncturtle.services.user.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.exception.RefreshSessionException;
import com.syncturtle.services.user.model.InstanceLite;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceLiteRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.RefreshSessionService;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionReceipt;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionSnapshot;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionStore;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshSessionServiceImpl implements RefreshSessionService {

    private final RefreshSessionStore refreshSessionStore;
    private final InstanceAuthorizationResolver authorizationResolver;
    private final AuthenticatedSessionIssuer authenticatedSessionIssuer;
    private final UserRepository userRepository;
    private final InstanceLiteRepository instanceRepository;

    @Override
    @Transactional
    public IssueTokenResponse refreshSession(String presentedRefreshToken) {
        if (!StringUtils.hasText(presentedRefreshToken)) {
            throw invalidRefresh("Missing refresh token");
        }

        try {
            RefreshSessionSnapshot snapshot = refreshSessionStore.requirePresented(presentedRefreshToken);
            RefreshSessionRecord record = snapshot.getRecord();
            User user = userRepository.findById(UUID.fromString(record.getUserId())).orElse(null);

            if (user == null || !user.isLoginAllowed()
                    || !Objects.equals(user.getAuthVersion(), record.getAuthVersion())) {
                throw invalidRefresh("User session is no longer valid");
            }

            InstanceLite instance = instanceRepository.findById(UUID.fromString(record.getInstanceId())).orElse(null);
            if (instance == null || !instance.isSetupDone()) {
                throw invalidRefresh("Instance is unavailable");
            }

            InstanceAuthorizationSnapshot authorization = authorizationResolver.resolve(user.getId(), instance.getId());
            List<String> currentRoles = new ArrayList<>(authorization.getRoles());
            Collections.sort(currentRoles);

            if (!record.getRoles().equals(currentRoles)
                    || !Objects.equals(record.getAdminSessionVersion(), authorization.getAdminSessionVersion())) {
                throw invalidRefresh("Authorization changed");
            }

            RefreshSessionReceipt rotated = refreshSessionStore.rotate(presentedRefreshToken, snapshot);
            AuthenticatedSessionReceipt session = authenticatedSessionIssuer.completeSession(rotated);

            return IssueTokenResponse.issued(session, null);
        } catch (RefreshSessionException exception) {
            if (exception.getReason() == RefreshSessionException.Reason.UNAVAILABLE) {
                throw RemoteServiceException.unavailable("session-state", exception);
            }

            throw invalidRefresh("Refresh session is no longer valid");
        }
    }

    private static AuthException invalidRefresh(String message) {
        return AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED)
                .with("message", message);
    }

}

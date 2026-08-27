package com.syncturtle.services.user.service.impl;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.dto.response.UserSessionInventoryResponse;
import com.syncturtle.services.user.dto.response.UserSessionResponse;
import com.syncturtle.services.user.dto.response.UserSessionRevocationResponse;
import com.syncturtle.services.user.exception.RefreshSessionLifecycleException;
import com.syncturtle.services.user.service.UserSessionService;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionFamilySnapshot;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionFamilyStore;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private static final String INSTANCE_ADMIN = "INSTANCE_ADMIN";

    private final RefreshSessionFamilyStore familyStore;
    private final Clock clock;

    @Override
    public UserSessionInventoryResponse listSessions(UUID userId, UUID currentSessionId) {
        requireIds(userId, currentSessionId);

        try {
            List<RefreshSessionFamilySnapshot> snapshots = familyStore.inventory(
                    userId.toString(),
                    currentSessionId.toString(),
                    clock.instant());
            List<UserSessionResponse> sessions = mapSessions(snapshots, currentSessionId);
            return new UserSessionInventoryResponse(sessions);
        } catch (RefreshSessionLifecycleException exception) {
            throw mapLifecycleFailure(exception);
        }
    }

    @Override
    public UserSessionRevocationResponse revokeSession(UUID userId, UUID currentSessionId, UUID targetSessionId) {
        requireIds(userId, currentSessionId);
        Assert.notNull(targetSessionId, "targetSessionId is required");

        try {
            familyStore.revokeOne(userId.toString(), targetSessionId.toString());
            return new UserSessionRevocationResponse(targetSessionId.equals(currentSessionId));
        } catch (RefreshSessionLifecycleException exception) {
            throw mapLifecycleFailure(exception);
        }
    }

    @Override
    public UserSessionRevocationResponse revokeOtherSessions(UUID userId, UUID currentSessionId) {
        requireIds(userId, currentSessionId);

        try {
            familyStore.revokeOthers(userId.toString(), currentSessionId.toString());
            return new UserSessionRevocationResponse(false);
        } catch (RefreshSessionLifecycleException exception) {
            throw mapLifecycleFailure(exception);
        }
    }

    @Override
    public UserSessionRevocationResponse revokeAllSessions(UUID userId, UUID currentSessionId) {
        requireIds(userId, currentSessionId);

        try {
            familyStore.revokeAll(userId.toString(), currentSessionId.toString());
            return new UserSessionRevocationResponse(true);
        } catch (RefreshSessionLifecycleException exception) {
            throw mapLifecycleFailure(exception);
        }
    }

    private static List<UserSessionResponse> mapSessions(
            List<RefreshSessionFamilySnapshot> snapshots,
            UUID currentSessionId) {
        List<RefreshSessionFamilySnapshot> ordered = new ArrayList<>(snapshots);
        ordered.sort(snapshotOrder());

        List<UserSessionResponse> sessions = new ArrayList<>(ordered.size());
        for (RefreshSessionFamilySnapshot snapshot : ordered) {
            RefreshSessionFamilyRecord family = snapshot.getFamily();
            boolean administrator = family.getAdminSessionVersion() != null
                    && family.getRoles().contains(INSTANCE_ADMIN);
            UserSessionResponse response = UserSessionResponse.builder()
                    .sessionId(snapshot.getSessionId())
                    .current(currentSessionId.equals(snapshot.getSessionId()))
                    .administrator(administrator)
                    .createdAt(family.getCreatedAt())
                    .lastUsedAt(family.getLastUsedAt())
                    .idleExpiresAt(family.getIdleExpiresAt())
                    .absoluteExpiresAt(family.getAbsoluteExpiresAt())
                    .build();
            sessions.add(response);
        }

        return sessions;
    }

    private static Comparator<RefreshSessionFamilySnapshot> snapshotOrder() {
        return (first, second) -> {
            int activityOrder = second.getFamily().getLastUsedAt()
                    .compareTo(first.getFamily().getLastUsedAt());
            if (activityOrder != 0) {
                return activityOrder;
            }
            return first.getSessionId().toString().compareTo(second.getSessionId().toString());
        };
    }

    private static RuntimeException mapLifecycleFailure(RefreshSessionLifecycleException exception) {
        if (exception.getReason() == RefreshSessionLifecycleException.Reason.MISSING_FAMILY
                || exception.getReason() == RefreshSessionLifecycleException.Reason.EXPIRED_FAMILY) {
            return AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED, exception)
                    .with("message", "Authenticated session is no longer valid");
        }

        return RemoteServiceException.unavailable("session-state", exception);
    }

    private static void requireIds(UUID userId, UUID currentSessionId) {
        Assert.notNull(userId, "userId is required");
        Assert.notNull(currentSessionId, "currentSessionId is required");
    }

}

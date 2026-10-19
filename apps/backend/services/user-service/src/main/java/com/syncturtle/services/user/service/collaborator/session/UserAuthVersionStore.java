package com.syncturtle.services.user.service.collaborator.session;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.services.user.exception.RefreshSessionException;
import com.syncturtle.services.user.exception.RefreshSessionException.Reason;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class UserAuthVersionStore {

    private static final int MAX_ADVANCE_ATTEMPTS = 5;

    private final StringRedisTemplate redis;
    private final RefreshSessionRedisKeys keys;

    public void ensureUserCurrent(String userId, long expectedVersion) {
        ensureCurrent(keys.userVersion(userId), expectedVersion);
    }

    public void ensureAdminCurrent(String instanceId, String userId, Long expectedVersion) {
        if (expectedVersion != null) {
            ensureCurrent(keys.adminVersion(instanceId, userId), expectedVersion);
        }
    }

    public void requireCurrent(RefreshSessionSnapshot snapshot) {
        Assert.notNull(snapshot, "snapshot is required");

        RefreshSessionRecord record = snapshot.getRecord();
        requireVersion(keys.userVersion(record.getUserId()), record.getAuthVersion());

        if (record.getAdminSessionVersion() != null) {
            requireVersion(keys.adminVersion(record.getInstanceId(), record.getUserId()),
                    record.getAdminSessionVersion());
        }
    }

    public void advanceUser(String userId, long newVersion) {
        Assert.hasText(userId, "userId is required");
        Assert.isTrue(newVersion >= 0, "newVersion must be nonnegative");

        String key = keys.userVersion(userId);
        String next = Long.toString(newVersion);

        try {
            for (int attempt = 0; attempt < MAX_ADVANCE_ATTEMPTS; attempt++) {
                String current = redis.opsForValue().get(key);
                if (current == null) {
                    if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, next))) {
                        return;
                    }

                    continue;
                }

                long currentVersion = parseVersion(current);
                if (currentVersion > newVersion) {
                    throw new RefreshSessionException(Reason.INVALID, "user version is newer than the DB snapshot");
                }

                if (currentVersion == newVersion) {
                    return;
                }

                if (Boolean.TRUE.equals(redis.opsForValue().compareAndSet(key, current, next))) {
                    return;
                }
            }
        } catch (DataAccessException exception) {
            throw new RefreshSessionException(Reason.UNAVAILABLE, "user version store is unavailable", exception);
        }

        throw new RefreshSessionException(Reason.UNAVAILABLE, "user version changed repeatedly");
    }

    private void ensureCurrent(String key, long expectedVersion) {
        Assert.isTrue(expectedVersion >= 0, "expectedVersion must be nonnegative");

        try {
            String current = redis.opsForValue().get(key);
            if (current == null) {
                redis.opsForValue().setIfAbsent(key, Long.toString(expectedVersion));
            }

            requireVersion(key, expectedVersion);
        } catch (DataAccessException exception) {
            throw new RefreshSessionException(Reason.UNAVAILABLE, "version store is unavailable", exception);
        }
    }

    private void requireVersion(String key, long expectedVersion) {
        try {
            String current = redis.opsForValue().get(key);
            if (current == null || parseVersion(current) != expectedVersion) {
                throw new RefreshSessionException(Reason.INVALID, "session security version is stale");
            }
        } catch (DataAccessException exception) {
            throw new RefreshSessionException(Reason.UNAVAILABLE, "version store is unavailable", exception);
        }
    }

    private static long parseVersion(String value) {
        try {
            long version = Long.parseLong(value);
            if (version < 0) {
                throw new NumberFormatException("negative version");
            }

            return version;
        } catch (NumberFormatException exception) {
            throw new RefreshSessionException(Reason.INVALID, "malformed security version", exception);
        }
    }

}

package com.syncturtle.common.contracts.user.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.syncturtle.common.contracts.user.error.UserErrorCode;
import com.syncturtle.common.core.exception.SyncturtleServiceException;

public final class UserException extends SyncturtleServiceException {

    private final UserErrorCode userErrorCode;

    private UserException(UserErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
        this.userErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
    }

    public UserErrorCode getUserErrorCode() {
        return userErrorCode;
    }

    public static UserException of(UserErrorCode errorCode) {
        return new UserException(errorCode, Map.of(), null);
    }

    public static UserException of(UserErrorCode errorCode, Throwable cause) {
        return new UserException(errorCode, Map.of(), cause);
    }

    public static UserException userNotFound(UUID userId) {
        return new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("user_id", userId), null);
    }

    public static UserException profileNotFound(UUID userId) {
        return new UserException(UserErrorCode.USER_PROFILE_NOT_FOUND, Map.of("user_id", userId), null);
    }

    public static UserException assetInvalid(UUID assetId) {
        return new UserException(UserErrorCode.USER_ASSET_INVALID, Map.of("asset_id", assetId), null);
    }

    public static UserException assetNotUploaded(UUID assetId) {
        return new UserException(UserErrorCode.USER_ASSET_NOT_UPLOADED, Map.of("asset_id", assetId), null);
    }

    public static UserException assetForbidden(UUID assetId) {
        return new UserException(UserErrorCode.USER_ASSET_FORBIDDEN, Map.of("asset_id", assetId), null);
    }

    public UserException with(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(getPayload());

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return new UserException(userErrorCode, copy, getCause());
    }

}

package com.syncturtle.services.user.support.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Objects;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;

public final class AuthExceptionAssert extends AbstractObjectAssert<AuthExceptionAssert, AuthException> {

    private AuthExceptionAssert(AuthException actual) {
        super(actual, AuthExceptionAssert.class);
    }

    public static AuthExceptionAssert assertThatAuthException(AuthException actual) {
        return new AuthExceptionAssert(actual);
    }

    public static AuthExceptionAssert assertThatAuthExceptionThrownBy(ThrowingCallable callable) {
        Throwable throwable = catchThrowable(callable);

        assertThat(throwable)
                .as("expected an AuthException to be thrown")
                .isInstanceOf(AuthException.class);

        return assertThatAuthException((AuthException) throwable);
    }

    public AuthExceptionAssert hasErrorCode(AuthErrorCode expectedErrorCode) {
        isNotNull();

        AuthErrorCode actualErrorCode = actual.getAuthErrorCode();
        if (actualErrorCode != expectedErrorCode) {
            failWithMessage("Expected auth error code <%s> but was <%s>", expectedErrorCode, actualErrorCode);
        }

        return this;
    }

    public AuthExceptionAssert hasPayloadEntry(String key, Object expectedValue) {
        isNotNull();

        Object actualValue = actual.getPayload().get(key);
        if (!Objects.equals(actualValue, expectedValue)) {
            failWithMessage("Expected payload entry <%s=$s> but was <%s=%s>", key, expectedValue, key, actualValue);
        }

        return this;
    }

    public AuthExceptionAssert hasNoPayload() {
        isNotNull();

        if (!actual.getPayload().isEmpty()) {
            failWithMessage("Expected no auth exception payload but was <%s>", actual.getPayload());
        }

        return this;
    }

}

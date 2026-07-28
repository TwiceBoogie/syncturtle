package com.syncturtle.services.email.support.assertion;

import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleServiceException;

public final class EmailExceptionAssert
        extends AbstractThrowableAssert<EmailExceptionAssert, SyncturtleServiceException> {

    private EmailExceptionAssert(SyncturtleServiceException actual) {
        super(actual, EmailExceptionAssert.class);
    }

    public static EmailExceptionAssert assertThatEmailExceptionThrownBy(ThrowingCallable callable) {
        SyncturtleServiceException exception = catchThrowableOfType(SyncturtleServiceException.class, callable);
        return new EmailExceptionAssert(exception);
    }

    public EmailExceptionAssert hasErrorCode(EmailErrorCode expected) {
        isNotNull();
        if (actual.getErrorCode() != expected) {
            failWithMessage("Expected error code <%s> but was <%s>", expected, actual.getErrorCode());
        }
        return this;
    }

    public EmailExceptionAssert hasNoPayload() {
        isNotNull();
        if (!actual.getPayload().isEmpty()) {
            failWithMessage("Expected no payload but was <%s>", actual.getPayload());
        }
        return this;
    }

    public EmailExceptionAssert hasPayloadEntry(String key, Object value) {
        isNotNull();
        if (!actual.getPayload().containsKey(key) || !java.util.Objects.equals(actual.getPayload().get(key), value)) {
            failWithMessage("Expected payload entry <%s=%s> but was <%s>", key, value, actual.getPayload());
        }
        return this;
    }

    public EmailExceptionAssert hasCauseSameAs(Throwable cause) {
        isNotNull();
        if (actual.getCause() != cause) {
            failWithMessage("Expected cause <%s> but was <%s>", cause, actual.getCause());
        }
        return this;
    }
}

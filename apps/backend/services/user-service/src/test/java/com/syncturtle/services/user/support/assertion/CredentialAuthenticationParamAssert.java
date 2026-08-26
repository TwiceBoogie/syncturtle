package com.syncturtle.services.user.support.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import com.syncturtle.services.user.service.param.CredentialAuthenticationParam;

public final class CredentialAuthenticationParamAssert {

    private final CredentialAuthenticationParam actual;

    private CredentialAuthenticationParamAssert(CredentialAuthenticationParam actual) {
        assertThat(actual).isNotNull();

        this.actual = actual;
    }

    public static CredentialAuthenticationParamAssert assertThatCredentialParam(CredentialAuthenticationParam actual) {
        return new CredentialAuthenticationParamAssert(actual);
    }

    public CredentialAuthenticationParamAssert hasEmail(String expected) {
        assertThat(actual.getEmail()).isEqualTo(expected);
        return this;
    }

    public CredentialAuthenticationParamAssert hasSecret(String expected) {
        assertThat(actual.getSecret()).isEqualTo(expected);
        return this;
    }

    public CredentialAuthenticationParamAssert hasSignup(boolean expected) {
        assertThat(actual.isSignup()).isEqualTo(expected);
        return this;
    }

    public CredentialAuthenticationParamAssert hasIpAddress(String expected) {
        assertThat(actual.getIpAddress()).isEqualTo(expected);
        return this;
    }

    public CredentialAuthenticationParamAssert hasUserAgent(String expected) {
        assertThat(actual.getUserAgent()).isEqualTo(expected);
        return this;
    }

}

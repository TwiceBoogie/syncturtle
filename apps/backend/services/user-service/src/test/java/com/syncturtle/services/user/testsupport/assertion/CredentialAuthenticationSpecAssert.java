package com.syncturtle.services.user.testsupport.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import com.syncturtle.services.user.service.authentication.provider.CredentialAuthenticationSpec;

public final class CredentialAuthenticationSpecAssert {

    private final CredentialAuthenticationSpec actual;

    private CredentialAuthenticationSpecAssert(CredentialAuthenticationSpec actual) {
        assertThat(actual).isNotNull();

        this.actual = actual;
    }

    public static CredentialAuthenticationSpecAssert assertThatCredentialSpec(CredentialAuthenticationSpec actual) {
        return new CredentialAuthenticationSpecAssert(actual);
    }

    public CredentialAuthenticationSpecAssert hasEmail(String expected) {
        assertThat(actual.getEmail()).isEqualTo(expected);
        return this;
    }

    public CredentialAuthenticationSpecAssert hasSecret(String expected) {
        assertThat(actual.getSecret()).isEqualTo(expected);
        return this;
    }

    public CredentialAuthenticationSpecAssert hasSignup(boolean expected) {
        assertThat(actual.isSignup()).isEqualTo(expected);
        return this;
    }

    public CredentialAuthenticationSpecAssert hasIpAddress(String expected) {
        assertThat(actual.getIpAddress()).isEqualTo(expected);
        return this;
    }

    public CredentialAuthenticationSpecAssert hasUserAgent(String expected) {
        assertThat(actual.getUserAgent()).isEqualTo(expected);
        return this;
    }

}

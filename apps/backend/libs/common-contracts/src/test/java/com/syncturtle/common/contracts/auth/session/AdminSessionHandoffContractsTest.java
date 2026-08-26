package com.syncturtle.common.contracts.auth.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Administrator handoff shared contracts")
class AdminSessionHandoffContractsTest {

    @Test
    @DisplayName("contain no raw browser credential fields")
    void containNoRawBrowserCredentialFields() {
        // arrange
        Class<?>[] contracts = {
                CreateInstanceAdminSessionHandoffRequest.class,
                AdminSessionHandoffResponse.class,
                PreAuthTransactionBinding.class
        };
        // act
        String[] fields = Arrays.stream(contracts)
                .flatMap(type -> Arrays.stream(type.getDeclaredFields()))
                .map(Field::getName)
                .toArray(String[]::new);
        // assert
        assertThat(fields).doesNotContain(
                "accessToken",
                "refreshToken",
                "signedCsrfToken",
                "refreshTokenHash",
                "accessTokenHash");
    }

}

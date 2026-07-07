package com.syncturtle.services.user.service.authentication.oauth;

import java.util.Optional;

import com.syncturtle.common.contracts.auth.provider.AuthProvider;

public interface OAuthStateStore {
    void save(OAuthState state, AuthProvider provider);

    Optional<OAuthState> consume(String state, AuthProvider provider);
}

package com.syncturtle.services.user.service.authentication.provider;

import com.syncturtle.services.user.type.CredentialProviderType;

public interface CredentialProvider {
    CredentialProviderType provider();

    CredentialAuthenticationReceipt authenticate(CredentialAuthenticationSpec param);
}

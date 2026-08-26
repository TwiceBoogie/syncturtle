package com.syncturtle.services.user.service.collaborator.authentication.provider;

import com.syncturtle.services.user.service.param.CredentialAuthenticationParam;
import com.syncturtle.services.user.type.CredentialProviderType;

public interface CredentialProvider {
    CredentialProviderType provider();

    CredentialAuthenticationReceipt authenticate(CredentialAuthenticationParam param);
}

package com.syncturtle.services.user.dto.response;

import com.syncturtle.services.user.service.collaborator.session.IssuedRefreshTokenReceipt;
import com.syncturtle.services.user.service.collaborator.token.IssuedAccessTokenReceipt;

import lombok.Builder;
import lombok.Value;

@Value
public class RefreshExchangeResponse {
    IssuedAccessTokenReceipt access;
    IssuedRefreshTokenReceipt refresh;

    @Builder
    private RefreshExchangeResponse(
            IssuedAccessTokenReceipt access,
            IssuedRefreshTokenReceipt refresh) {
        this.access = access;
        this.refresh = refresh;
    }
}

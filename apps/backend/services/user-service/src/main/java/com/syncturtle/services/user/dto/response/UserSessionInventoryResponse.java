package com.syncturtle.services.user.dto.response;

import java.util.List;

import lombok.Getter;

@Getter
public final class UserSessionInventoryResponse {

    private final List<UserSessionResponse> sessions;

    public UserSessionInventoryResponse(List<UserSessionResponse> sessions) {
        this.sessions = List.copyOf(sessions);
    }

}

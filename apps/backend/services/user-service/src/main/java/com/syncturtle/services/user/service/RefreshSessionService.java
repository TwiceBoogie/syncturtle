package com.syncturtle.services.user.service;

import com.syncturtle.services.user.dto.response.IssueTokenResponse;

public interface RefreshSessionService {
    IssueTokenResponse refreshSession(String presentedRefreshToken);
}

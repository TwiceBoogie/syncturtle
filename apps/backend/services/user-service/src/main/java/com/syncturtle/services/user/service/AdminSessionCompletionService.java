package com.syncturtle.services.user.service;

import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.service.param.AdminSessionCompletionParam;

public interface AdminSessionCompletionService {
    IssueTokenResponse complete(AdminSessionCompletionParam param);
}

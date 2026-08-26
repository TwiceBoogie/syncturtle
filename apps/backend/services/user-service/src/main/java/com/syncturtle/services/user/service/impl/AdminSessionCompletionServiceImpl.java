package com.syncturtle.services.user.service.impl;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.PreAuthTransactionBinding;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.exception.AdminSessionHandoffException;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.AdminSessionCompletionService;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffClaim;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffRecord;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffStore;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionClientFingerprint;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionClientFingerprintFactory;
import com.syncturtle.services.user.service.param.AdminSessionCompletionParam;
import com.syncturtle.services.user.service.param.AuthorizedSessionIssueParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSessionCompletionServiceImpl implements AdminSessionCompletionService {

    private final AdminSessionHandoffStore handoffStore;
    private final UserRepository userRepository;
    private final InstanceAuthorizationResolver authorizationResolver;
    private final RefreshSessionClientFingerprintFactory fingerprintFactory;
    private final AuthenticatedSessionIssuer sessionIssuer;
    private final PublicUrlResolver hostResolver;

    @Override
    @Transactional(readOnly = true)
    public IssueTokenResponse complete(AdminSessionCompletionParam param) {
        Assert.notNull(param, "admin session completion param is required");

        String preAuthHash = PreAuthTransactionBinding.fromValidatedSignedCsrfToken(param.getSignedCsrfToken())
                .getValue();
        RefreshSessionClientFingerprint fingerprint = fingerprintFactory.create(param.getClientIp(),
                param.getUserAgent());
        AdminSessionHandoffClaim claim = handoffStore.claim(param.getCompletionCode(), preAuthHash,
                fingerprint.getClientBindingHash());
        AdminSessionHandoffRecord record = claim.getRecord();

        InstanceAuthorizationSnapshot authorization;
        try {
            authorization = revalidateAuthority(record, claim);
        } catch (AdminSessionHandoffException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            releaseForRetry(claim, exception);
            throw exception;
        }

        // Once issuer invocation begins, its outcome can be uncertain. Never release
        // the handoff claim after this point because retry could create a second family.
        AuthenticatedSessionReceipt session = sessionIssuer.issueAuthorizedSession(AuthorizedSessionIssueParam.builder()
                .userId(record.getUserId().toString())
                .instanceId(record.getInstanceId().toString())
                .roles(authorization.getRoles())
                .authVersion(record.getUserAuthVersion())
                .adminSessionVersion(authorization.getAdminSessionVersion())
                .ipAddress(param.getClientIp())
                .userAgent(param.getUserAgent())
                .build());

        consumeAfterSuccessfulIssuance(claim);
        return IssueTokenResponse.issued(session, hostResolver.admin("/general"));
    }

    private InstanceAuthorizationSnapshot revalidateAuthority(
            AdminSessionHandoffRecord record,
            AdminSessionHandoffClaim claim) {
        User user = userRepository.findById(record.getUserId()).orElse(null);
        if (user == null || !user.isLoginAllowed()
                || !Objects.equals(user.getAuthVersion(), record.getUserAuthVersion())) {
            throw invalidate(claim);
        }

        InstanceAuthorizationSnapshot authorization = authorizationResolver.resolve(record.getUserId(),
                record.getInstanceId());
        if (!authorization.isInstanceAdmin()
                || !Objects.equals(authorization.getAdminSessionVersion(), record.getAdminSessionVersion())) {
            throw invalidate(claim);
        }
        return authorization;
    }

    private AdminSessionHandoffException invalidate(AdminSessionHandoffClaim claim) {
        AdminSessionHandoffException invalid = new AdminSessionHandoffException(
                AdminSessionHandoffException.Reason.INVALID,
                "administrator session authorization is no longer valid");
        try {
            handoffStore.consume(claim);
        } catch (RuntimeException cleanupFailure) {
            invalid.addSuppressed(cleanupFailure);
        }
        return invalid;
    }

    private void releaseForRetry(AdminSessionHandoffClaim claim, RuntimeException original) {
        try {
            handoffStore.release(claim);
        } catch (RuntimeException releaseFailure) {
            original.addSuppressed(releaseFailure);
        }
    }

    private void consumeAfterSuccessfulIssuance(AdminSessionHandoffClaim claim) {
        try {
            handoffStore.consume(claim);
        } catch (RuntimeException ignored) {
            log.warn("Issued administrator session while handoff cleanup remains pending");
        }
    }

}

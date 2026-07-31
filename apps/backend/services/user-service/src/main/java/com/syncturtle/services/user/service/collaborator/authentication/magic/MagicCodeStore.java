package com.syncturtle.services.user.service.collaborator.authentication.magic;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;

public interface MagicCodeStore {
    MagicCodeChallenge createOrRotate(String email, AuthErrorCode attemptExhaustedErrorCode);

    VerificationResult verify(String email, String submittedCode);

    enum FailureReason {
        INVALID,
        EXPIRED
    }

    final class VerificationResult {
        private final boolean verified;
        private final FailureReason failureReason;

        private VerificationResult(boolean verified, FailureReason failureReason) {
            this.verified = verified;
            this.failureReason = failureReason;
        }

        public static VerificationResult verified() {
            return new VerificationResult(true, null);
        }

        public static VerificationResult failed(FailureReason failureReason) {
            return new VerificationResult(false, failureReason);
        }

        public boolean isVerified() {
            return verified;
        }

        public FailureReason getFailureReason() {
            return failureReason;
        }
    }
}

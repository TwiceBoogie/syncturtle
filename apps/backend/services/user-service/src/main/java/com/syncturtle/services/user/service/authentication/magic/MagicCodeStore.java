package com.syncturtle.services.user.service.authentication.magic;

public interface MagicCodeStore {
    MagicCodeChallenge createOrRotate(String email);

    VerificationResult verify(String email, String submittedCode);

    enum FailureReason {
        INVALID,
        EXPIRED,
        ATTEMPT_EXHAUSTED
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

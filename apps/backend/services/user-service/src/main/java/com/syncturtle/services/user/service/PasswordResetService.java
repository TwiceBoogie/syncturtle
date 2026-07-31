package com.syncturtle.services.user.service;

public interface PasswordResetService {
    void requestReset(String email);

    void resetPassword(String uidb64, String token, String password);
}

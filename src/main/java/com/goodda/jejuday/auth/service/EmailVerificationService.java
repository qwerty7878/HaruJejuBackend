package com.goodda.jejuday.auth.service;

import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.entity.VerificationType;

public interface EmailVerificationService {

    void saveVerificationCode(String email, String code, VerificationType type);

    void saveVerificationCodeForUser(User user, String code);

    boolean verifyCode(String email, String code, VerificationType type);

    boolean isEmailVerified(String email, VerificationType type);

    void deleteVerifiedCode(String email, VerificationType type);

    void cleanupExpiredVerifications();
}
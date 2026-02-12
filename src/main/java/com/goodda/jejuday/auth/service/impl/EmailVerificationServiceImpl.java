package com.goodda.jejuday.auth.service.impl;

import com.goodda.jejuday.auth.entity.EmailVerification;
import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.entity.VerificationType;
import com.goodda.jejuday.auth.repository.EmailVerificationRepository;
import com.goodda.jejuday.auth.service.EmailVerificationService;
import com.goodda.jejuday.common.exception.EmailSendingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;

    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 3;
    private static final int VERIFIED_CODE_RETENTION_MINUTES = 5;

//    이메일 인증 코드 저장 (회원가입 또는 비밀번호 재설정용)
    @Override
    @Transactional
    public void saveVerificationCode(String email, String code, VerificationType type) {
        // 기존 미인증 코드 삭제
        List<EmailVerification> existingCodes = emailVerificationRepository
                .findByEmailAndVerificationTypeAndIsVerifiedFalse(email, type);

        if (!existingCodes.isEmpty()) {
            emailVerificationRepository.deleteAll(existingCodes);
        }

        // 새 인증 코드 생성
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .verificationType(type)
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES))
                .build();

        emailVerificationRepository.save(verification);
    }

//    비밀번호 재설정용 인증 코드 저장 (User 연결)
    @Override
    @Transactional
    public void saveVerificationCodeForUser(User user, String code) {
        // 기존 미인증 코드 삭제
        List<EmailVerification> existingCodes = emailVerificationRepository
                .findByEmailAndVerificationTypeAndIsVerifiedFalse(
                        user.getEmail(), VerificationType.PASSWORD_RESET);

        if (!existingCodes.isEmpty()) {
            emailVerificationRepository.deleteAll(existingCodes);
        }

        // 새 인증 코드 생성
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .email(user.getEmail())
                .verificationCode(code)
                .verificationType(VerificationType.PASSWORD_RESET)
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES))
                .build();

        emailVerificationRepository.save(verification);
        log.info("Password reset verification code saved for user: {}", user.getEmail());
    }

//    인증 코드 검증
    @Override
    @Transactional
    public boolean verifyCode(String email, String code, VerificationType type) {
        // 가장 최근 미인증 코드 조회
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndVerificationTypeAndIsVerifiedFalseOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new EmailSendingException("인증 기록이 없습니다."));

        // 만료 확인
        if (verification.isExpired()) {
            throw new EmailSendingException("인증 코드가 만료되었습니다. 새로운 인증 코드를 요청해주세요.");
        }

        // 코드 일치 확인
        if (!verification.getVerificationCode().equals(code)) {
            throw new EmailSendingException("인증 코드가 일치하지 않습니다.");
        }

        // 인증 완료 처리
        verification.markAsVerified();
        emailVerificationRepository.save(verification);

        return true;
    }

//    인증 완료 여부 확인 (회원가입 시 사용)
    @Override
    @Transactional(readOnly = true)
    public boolean isEmailVerified(String email, VerificationType type) {
        return emailVerificationRepository
                .findTopByEmailAndVerificationTypeAndIsVerifiedTrueOrderByCreatedAtDesc(email, type)
                .filter(verification -> {
                    // 인증 완료 후 5분 이내만 유효
                    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(VERIFIED_CODE_RETENTION_MINUTES);
                    return verification.getVerifiedAt() != null
                            && verification.getVerifiedAt().isAfter(cutoff);
                })
                .isPresent();
    }

//    인증 완료된 코드 삭제 (회원가입 완료 후 호출)
    @Override
    @Transactional
    public void deleteVerifiedCode(String email, VerificationType type) {
        emailVerificationRepository
                .findTopByEmailAndVerificationTypeAndIsVerifiedTrueOrderByCreatedAtDesc(email, type)
                .ifPresent(verification -> {
                    emailVerificationRepository.delete(verification);
                });
    }

//    만료된 인증 코드 정리 (스케줄러에서 호출)
    @Override
    @Transactional
    public void cleanupExpiredVerifications() {
        // 만료된 미인증 코드 삭제
        emailVerificationRepository.deleteExpiredVerifications(LocalDateTime.now());

        // 1일 이전의 인증 완료된 코드 삭제
        emailVerificationRepository.deleteOldVerifiedCodes(LocalDateTime.now().minusDays(1));
    }
}
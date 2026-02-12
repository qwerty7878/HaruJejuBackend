package com.goodda.jejuday.auth.repository;

import com.goodda.jejuday.auth.entity.EmailVerification;
import com.goodda.jejuday.auth.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 이메일과 타입으로 가장 최근 미인증 코드 조회
    Optional<EmailVerification> findTopByEmailAndVerificationTypeAndIsVerifiedFalseOrderByCreatedAtDesc(
            String email, VerificationType verificationType);

    // 이메일과 타입으로 가장 최근 인증 완료된 코드 조회
    Optional<EmailVerification> findTopByEmailAndVerificationTypeAndIsVerifiedTrueOrderByCreatedAtDesc(
            String email, VerificationType verificationType);

    // User ID로 가장 최근 인증 코드 조회 (비밀번호 재설정용)
    Optional<EmailVerification> findTopByUserIdAndVerificationTypeAndIsVerifiedFalseOrderByCreatedAtDesc(
            Long userId, VerificationType verificationType);

    // 이메일과 타입으로 미인증 코드 목록 조회
    List<EmailVerification> findByEmailAndVerificationTypeAndIsVerifiedFalse(
            String email, VerificationType verificationType);

    // 만료된 인증 코드 삭제
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    void deleteExpiredVerifications(@Param("now") LocalDateTime now);

    // 특정 기간 이전의 인증 완료된 코드 삭제
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.isVerified = true AND e.verifiedAt < :cutoff")
    void deleteOldVerifiedCodes(@Param("cutoff") LocalDateTime cutoff);
}
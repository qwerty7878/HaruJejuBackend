package com.goodda.jejuday.auth.service;

import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.entity.VerificationType;
import com.goodda.jejuday.auth.repository.UserRepository;
import com.goodda.jejuday.common.exception.DuplicateEmailException;
import com.goodda.jejuday.common.exception.EmailSendingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;

    private static final SecureRandom random = new SecureRandom();

//    회원가입용 이메일 인증 코드 발송
    @Transactional
    public void sendSignUpVerificationEmail(String email) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }

        // 인증 코드 생성
        String code = generateCode();

        // 인증 정보 저장
        emailVerificationService.saveVerificationCode(email, code, VerificationType.SIGNUP);

        // 이메일 발송
        String html = buildVerificationEmailHtml(code, "회원가입");
        sendEmail(email, "[제주데이] 회원가입 인증 코드", html);
    }

//    비밀번호 재설정용 이메일 인증 코드 발송
    @Transactional
    public void sendPasswordResetEmail(String email) {
        // 사용자 존재 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));

        // 인증 코드 생성
        String code = generateCode();

        // 인증 정보 저장 (User와 연결)
        emailVerificationService.saveVerificationCodeForUser(user, code);

        // 이메일 발송
        String html = buildVerificationEmailHtml(code, "비밀번호 재설정");
        sendEmail(email, "[제주데이] 비밀번호 재설정 인증 코드", html);
    }

//    실제 이메일 발송
    private void sendEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom("noreply@jejuday.com");

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new EmailSendingException("이메일 전송에 실패했습니다.");
        }
    }

//    6자리 인증 코드 생성 (SecureRandom 사용)
    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

//    Thymeleaf 템플릿으로 이메일 HTML 생성
    private String buildVerificationEmailHtml(String code, String purpose) {
        Context context = new Context();
        context.setVariable("verificationCode", code);
        context.setVariable("purpose", purpose);
        context.setVariable("expiryMinutes", 3);

        return templateEngine.process("verificationEmail", context);
    }
}
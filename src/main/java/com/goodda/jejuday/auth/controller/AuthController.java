package com.goodda.jejuday.auth.controller;

import com.goodda.jejuday.auth.dto.ApiResponse;
import com.goodda.jejuday.auth.dto.login.request.LoginRequest;
import com.goodda.jejuday.auth.dto.login.response.LoginResponse;
import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.entity.VerificationType;
import com.goodda.jejuday.auth.repository.UserRepository;
import com.goodda.jejuday.auth.security.JwtService;
import com.goodda.jejuday.auth.service.EmailService;
import com.goodda.jejuday.auth.service.EmailVerificationService;
import com.goodda.jejuday.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "로그인 및 비밀번호 재설정 API")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/users/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Operation(summary = "일반 로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        User user = userService.getUserByEmailOrNull(request.getEmail());

        if (user == null || !userService.matchesPassword(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 이메일 인증 확인
        if (!user.isEmailVerified()) {
            throw new IllegalStateException("이메일 인증이 완료되지 않은 계정입니다.");
        }

        // 계정 활성화 확인
        if (!user.isActive()) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        userService.setLoginCookie(response, user.getEmail());
        return ResponseEntity.ok(userService.loginResponse(user));
    }

    @Operation(
            summary = "비밀번호 재설정 인증 코드 전송",
            description = "비밀번호 재설정을 위한 인증 코드를 이메일로 발송합니다."
    )
    @PostMapping("/password/reset/send")
    public ResponseEntity<ApiResponse<String>> sendPasswordResetCode(@RequestParam String email) {
        // 사용자 존재 확인
        User user = userService.getUserByEmailOrNull(email);
        if (user == null) {
            throw new IllegalArgumentException("등록되지 않은 이메일입니다.");
        }

        // 인증 코드 발송
        emailService.sendPasswordResetEmail(email);

        return ResponseEntity.ok(
                ApiResponse.onSuccess("비밀번호 재설정 인증 코드가 이메일로 전송되었습니다.")
        );
    }

    @Operation(
            summary = "비밀번호 재설정 인증 코드 확인",
            description = "전송된 인증 코드를 검증합니다."
    )
    @PostMapping("/password/reset/verify")
    public ResponseEntity<ApiResponse<String>> verifyPasswordResetCode(
            @RequestParam String email,
            @RequestParam String code) {

        emailVerificationService.verifyCode(email, code, VerificationType.PASSWORD_RESET);

        return ResponseEntity.ok(ApiResponse.onSuccess("인증 코드 확인 완료"));
    }

    @Operation(
            summary = "비밀번호 재설정 완료",
            description = "인증 완료 후 새 비밀번호로 변경합니다."
    )
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword) {

        // 인증 완료 여부 확인
        if (!emailVerificationService.isEmailVerified(email, VerificationType.PASSWORD_RESET)) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        // 비밀번호 변경
        userService.resetPassword(email, newPassword);

        // 사용한 인증 코드 삭제
        emailVerificationService.deleteVerifiedCode(email, VerificationType.PASSWORD_RESET);

        return ResponseEntity.ok(ApiResponse.onSuccess("비밀번호가 성공적으로 변경되었습니다."));
    }

    @Operation(summary = "로그아웃", description = "JWT 쿠키를 삭제하고 FCM 토큰을 제거하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        jwtService.clearAccessTokenCookie(response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            userService.logoutUser(user.getId(), response);
        }

        return ResponseEntity.ok(ApiResponse.onSuccess("로그아웃 성공"));
    }
}
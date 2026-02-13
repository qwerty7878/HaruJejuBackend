package com.goodda.jejuday.auth.controller;

import com.goodda.jejuday.auth.dto.ApiResponse;
import com.goodda.jejuday.auth.dto.login.response.LoginResponse;
import com.goodda.jejuday.auth.dto.register.request.EmailSenderRequest;
import com.goodda.jejuday.auth.dto.register.request.EmailValidationRequest;
import com.goodda.jejuday.auth.dto.register.request.SignUpRequest;
import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.entity.VerificationType;
import com.goodda.jejuday.auth.service.EmailService;
import com.goodda.jejuday.auth.service.EmailVerificationService;
import com.goodda.jejuday.auth.service.KakaoService;
import com.goodda.jejuday.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Set;

@Tag(name = "Register", description = "회원가입 관련 API")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/users/register")
@RequiredArgsConstructor
public class RegisterController {

    private final UserService userService;
    private final KakaoService kakaoService;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "[1단계] 이메일 인증 코드 전송",
            description = "회원가입을 위한 이메일 인증 코드를 발송합니다."
    )
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<String>> sendVerificationEmail(
            @Valid @RequestBody EmailValidationRequest request) {

        // 이메일 중복 체크
        if (userService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 인증 코드 발송
        emailService.sendSignUpVerificationEmail(request.getEmail());

        return ResponseEntity.ok(ApiResponse.onSuccess("인증 코드가 이메일로 전송되었습니다."));
    }

    @Operation(
            summary = "[2단계] 이메일 인증 코드 확인",
            description = "입력한 이메일 인증 코드를 검증합니다."
    )
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @Valid @RequestBody EmailSenderRequest request) {

        emailVerificationService.verifyCode(
                request.getEmail(),
                request.getCode(),
                VerificationType.SIGNUP
        );

        return ResponseEntity.ok(ApiResponse.onSuccess("이메일 인증이 완료되었습니다."));
    }

    @Operation(
            summary = "[3단계] 회원가입 완료",
            description = "모든 정보를 입력하여 회원가입을 완료합니다."
    )
    @PostMapping(value = "/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LoginResponse>> completeRegistration(
            @RequestPart("data") @Valid SignUpRequest request,
            @RequestPart(value = "profile", required = false) MultipartFile profile,
            HttpServletResponse response) {

        // 이메일 인증 완료 확인
        if (!emailVerificationService.isEmailVerified(request.getEmail(), VerificationType.SIGNUP)) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        // 닉네임 중복 체크
        if (userService.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 프로필 이미지 업로드 (선택)
        String profileImageUrl = Optional.ofNullable(profile)
                .filter(p -> !p.isEmpty())
                .map(userService::uploadProfileImage)
                .orElse(null);

        // 테마 설정
        Set<String> themes = Optional.ofNullable(request.getThemes())
                .map(Set::copyOf)
                .orElse(Set.of());

        // 회원가입 완료
        User user = userService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getNickname(),
                profileImageUrl,
                themes,
                request.getGender(),
                request.getBirthYear(),
                request.getReferrerNickname()
        );

        // 사용한 인증 코드 삭제
        emailVerificationService.deleteVerifiedCode(request.getEmail(), VerificationType.SIGNUP);

        // 로그인 처리
        userService.setLoginCookie(response, user.getEmail());
        kakaoService.authenticateUser(user);

        LoginResponse loginResponse = userService.loginResponse(user);

        return new ResponseEntity<>(
                ApiResponse.onSuccess(loginResponse),
                HttpStatus.CREATED
        );
    }

    @Operation(summary = "이메일 중복 확인", description = "이메일이 이미 존재하는지 확인합니다.")
    @GetMapping("/check/email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.onSuccess(exists));
    }

    @Operation(summary = "닉네임 중복 확인", description = "닉네임이 이미 존재하는지 확인합니다.")
    @GetMapping("/check/nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@RequestParam String nickname) {
        boolean exists = userService.existsByNickname(nickname);
        return ResponseEntity.ok(ApiResponse.onSuccess(exists));
    }
}
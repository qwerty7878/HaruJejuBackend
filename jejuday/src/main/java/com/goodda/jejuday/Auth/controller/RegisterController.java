package com.goodda.jejuday.Auth.controller;

import com.goodda.jejuday.Auth.dto.ApiResponse;
import com.goodda.jejuday.Auth.dto.register.request.EmailSenderRequest;
import com.goodda.jejuday.Auth.dto.register.request.EmailValidationRequest;
import com.goodda.jejuday.Auth.dto.register.request.FinalAppRegisterRequest;
import com.goodda.jejuday.Auth.dto.register.request.TempAppRegisterRequest;
import com.goodda.jejuday.Auth.entity.Language;
import com.goodda.jejuday.Auth.entity.Platform;
import com.goodda.jejuday.Auth.service.EmailService;
import com.goodda.jejuday.Auth.service.EmailVerificationService;
import com.goodda.jejuday.Auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Register", description = "회원가입 관련 API")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/users/register")
@RequiredArgsConstructor
public class RegisterController {

    private final UserService userService;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/language")
    @Operation(summary = "언어 설정", description = "사용자가 선택한 언어를 쿠키에 저장")
    public ResponseEntity<ApiResponse<String>> setLanguage(@RequestParam("language") Language language,
                                                           HttpServletResponse response) {

        Cookie languageCookie = new Cookie("language", language.name());
        languageCookie.setHttpOnly(true);
        languageCookie.setPath("/");
        languageCookie.setMaxAge(7 * 24 * 60 * 60); // 7일 유지

        response.addCookie(languageCookie);

        return ResponseEntity.ok(ApiResponse.onSuccess("설정된 언어 : " + language.name()));
    }

    @Operation(summary = "임시 사용자 등록", description = "앱 회원가입 시 임시 사용자로 저장합니다.")
    @PostMapping("/app")
    public ResponseEntity<ApiResponse<String>> registerAppUser(@Valid @RequestBody TempAppRegisterRequest request,
                                                               @CookieValue(value = "language", required = false) Language language) {
        userService.saveTemporaryUser(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                Platform.APP,
                language
        );
        return new ResponseEntity<>(ApiResponse.onSuccess("임시 사용자 저장 완료"), HttpStatus.CREATED);
    }

    @Operation(summary = "이메일 인증코드 전송", description = "회원가입을 위한 이메일 인증 코드를 발송합니다.")
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<String>> sendVerificationEmail(
            @Valid @RequestBody EmailValidationRequest request) {
        emailService.sendRegistrationVerificationEmail(request.getEmail());
        return new ResponseEntity<>(ApiResponse.onSuccess("이메일 전송 완료"), HttpStatus.OK);
    }

    @Operation(summary = "이메일 인증코드 검증", description = "입력한 이메일 인증 코드를 검증합니다.")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody EmailSenderRequest request) {
        boolean result = emailVerificationService.verifyTemporaryUserCode(request.getEmail(), request.getCode());
        if (!result) {
            throw new IllegalArgumentException("인증 코드 불일치");
        }
        return new ResponseEntity<>(ApiResponse.onSuccess("이메일 인증 완료"), HttpStatus.OK);
    }

    @Operation(summary = "회원가입 완료 처리", description = "모든 정보를 입력하고 프로필 이미지 포함하여 회원가입을 완료합니다.")
    @PostMapping(value = "/final", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> completeRegistration(
            @RequestPart("data") @Valid FinalAppRegisterRequest request,
            @RequestPart(value = "profile", required = false) MultipartFile profile,
            HttpServletResponse response) {

        String profileImageUrl = Optional.ofNullable(profile)
                .filter(p -> !p.isEmpty())
                .map(userService::uploadProfileImage)
                .orElse(null);

        Set<String> themes = Optional.ofNullable(request.getThemes())
                .map(Set::copyOf)
                .orElse(Set.of());

        userService.completeFinalRegistration(
                request.getEmail(),
                request.getNickname(),
                profileImageUrl,
                themes
        );

        userService.setLoginCookie(response, request.getEmail());

        return new ResponseEntity<>(ApiResponse.onSuccess("회원가입 완료"), HttpStatus.CREATED);
    }
}

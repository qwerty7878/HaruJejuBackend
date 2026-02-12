package com.goodda.jejuday.auth.service.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.goodda.jejuday.auth.dto.login.response.LoginResponse;
import com.goodda.jejuday.auth.entity.Gender;
import com.goodda.jejuday.auth.entity.Platform;
import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.entity.UserTheme;
import com.goodda.jejuday.auth.entity.VerificationType;
import com.goodda.jejuday.auth.repository.UserRepository;
import com.goodda.jejuday.auth.repository.UserThemeRepository;
import com.goodda.jejuday.auth.security.JwtService;
import com.goodda.jejuday.auth.service.EmailVerificationService;
import com.goodda.jejuday.auth.service.ReferralService;
import com.goodda.jejuday.auth.service.UserService;
import com.goodda.jejuday.common.exception.BadRequestException;
import com.goodda.jejuday.common.exception.CustomS3Exception;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    private final AmazonS3 amazonS3;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final UserThemeRepository userThemeRepository;
    private final ReferralService referralService;

    private static final SecureRandom random = new SecureRandom();

    // 사용자 조회
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    }

    @Override
    public User getUserByEmailOrNull(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다: " + userId));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByEmail(username);
    }

    @Override
    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            String email = ((UserDetails) authentication.getPrincipal()).getUsername();
            return getUserByEmail(email).getId();
        }
        throw new BadRequestException("인증되지 않은 사용자입니다.");
    }

    // 로그인 관련
    @Override
    public void setLoginCookie(HttpServletResponse response, String email) {
        jwtService.addAccessTokenCookie(response, email);
    }

    @Override
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public LoginResponse loginResponse(User user) {
        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profile(user.getProfile())
                .birthYear(user.getBirthYear())
                .platform(user.getPlatform())
                .themes(user.getUserThemes().stream()
                        .map(UserTheme::getName)
                        .toList())
                .build();
    }

    // 회원가입
    @Override
    @Transactional
    public User createUser(String email, String password, String nickname, String profile,
                           Set<String> themeNames, Gender gender, String birthYear,
                           String referrerNickname) {

        // 이메일 인증 확인
        if (!emailVerificationService.isEmailVerified(email, VerificationType.SIGNUP)) {
            throw new BadRequestException("이메일 인증이 완료되지 않았습니다.");
        }

        // 이메일 중복 확인
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("이미 가입된 이메일입니다.");
        }

        // 닉네임 중복 확인
        if (userRepository.existsByNickname(nickname)) {
            throw new BadRequestException("이미 사용 중인 닉네임입니다.");
        }

        // 테마 설정
        Set<UserTheme> userThemes = themeNames != null
                ? themeNames.stream()
                .map(name -> userThemeRepository.findByName(name)
                        .orElseGet(() -> userThemeRepository.save(
                                UserTheme.builder().name(name).build())))
                .collect(Collectors.toSet())
                : Set.of();

        // 사용자 생성
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .profile(profile)
                .platform(Platform.APP)
                .gender(gender)
                .birthYear(birthYear)
                .userThemes(userThemes)
                .isEmailVerified(true)  // 이메일 인증 완료
                .isActive(true)         // 계정 활성화
                .hallabong(0)
                .totalSteps(0L)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user created: {}", savedUser.getEmail());

        // 추천인 처리
        if (referrerNickname != null && !referrerNickname.trim().isEmpty()) {
            try {
                referralService.processSignupBonus(savedUser.getId(), referrerNickname);
                log.info("Referral processed for user: {}", savedUser.getEmail());
            } catch (Exception e) {
                log.warn("추천인 처리 중 오류 발생: {}", e.getMessage());
                // 추천인 처리 실패해도 회원가입은 성공으로 처리
            }
        }

        return savedUser;
    }

    // 비밀번호 관련
    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 인증 코드 생성 (6자리)
    @Override
    public String generateVerificationCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    // 프로필 이미지 관련
    @Override
    public String uploadProfileImage(MultipartFile profileImage) {
        validateProfileImage(profileImage);

        String key = generateImageKey(profileImage.getOriginalFilename());
        ObjectMetadata metadata = createMetadata(profileImage);

        uploadToS3(profileImage, key, metadata);

        return getImageUrl(key);
    }

    private void validateProfileImage(MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            throw new IllegalArgumentException("프로필 이미지가 비어있습니다.");
        }

        String originalFilename = profileImage.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }

        String contentType = profileImage.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
    }

    private String generateImageKey(String originalFilename) {
        String savedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        return "profile-images/" + savedFilename;
    }

    private ObjectMetadata createMetadata(MultipartFile profileImage) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(profileImage.getSize());
        metadata.setContentType(profileImage.getContentType());
        return metadata;
    }

    private void uploadToS3(MultipartFile profileImage, String key, ObjectMetadata metadata) {
        try {
            amazonS3.putObject(bucketName, key, profileImage.getInputStream(), metadata);
        } catch (IOException e) {
            throw new RuntimeException("S3 프로필 이미지 업로드에 실패했습니다.", e);
        }
    }

    private String getImageUrl(String key) {
        URL url = amazonS3.getUrl(bucketName, key);
        if (url == null) {
            throw new RuntimeException("S3에서 이미지 URL을 가져올 수 없습니다.");
        }
        return url.toString();
    }

    @Override
    public String getProfileImageUrl(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));
        return user.getProfile();
    }

    @Override
    public void updateUserProfileImage(Long userId, String newProfileUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));
        user.setProfile(newProfileUrl);
        userRepository.save(user);
    }

    @Override
    public void deleteFile(String fileUrl) {
        String key = extractFileName(fileUrl);
        log.debug("삭제 시도할 S3 key: {}", key);

        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
            log.info("S3 파일 삭제 완료: {}", key);
        } catch (AmazonServiceException e) {
            log.error("AmazonServiceException 발생: {}", e.getErrorMessage());
            throw new CustomS3Exception("S3 삭제 실패: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            log.error("SdkClientException 발생: {}", e.getMessage());
            throw new CustomS3Exception("S3 클라이언트 오류: " + e.getMessage(), e);
        }
    }

    private String extractFileName(String fileUrl) {
        String httpsPrefix = "https://jejudaybucket123.s3.amazonaws.com/";
        String s3Prefix = "s3://jejudaybucket123/";

        if (fileUrl.startsWith(httpsPrefix)) {
            return fileUrl.replace(httpsPrefix, "");
        } else if (fileUrl.startsWith(s3Prefix)) {
            return fileUrl.replace(s3Prefix, "");
        }
        return fileUrl;
    }

    // 회원 정보 업데이트
    @Override
    @Transactional
    public void updateNickname(Long userId, String newNickname) {
        if (userRepository.existsByNickname(newNickname)) {
            throw new BadRequestException("이미 사용 중인 닉네임입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        user.setNickname(newNickname);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserThemes(Long userId, Set<String> themeNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        Set<UserTheme> userThemes = themeNames.stream()
                .map(name -> userThemeRepository.findByName(name)
                        .orElseGet(() -> userThemeRepository.save(
                                UserTheme.builder().name(name).build())))
                .collect(Collectors.toSet());

        user.setUserThemes(userThemes);
        userRepository.save(user);
    }

    // 알림 관련
    @Override
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        user.setFcmToken(fcmToken);

        if (!user.isNotificationEnabled()) {
            user.setNotificationEnabled(true);
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateNotificationSetting(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        user.setNotificationEnabled(enabled);
        userRepository.save(user);
    }

    // 로그아웃 & 회원탈퇴
    @Override
    @Transactional
    public void logoutUser(Long userId, HttpServletResponse response) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        user.setFcmToken(null);
        userRepository.save(user);
        jwtService.clearAccessTokenCookie(response);
    }

    @Override
    @Transactional
    public void deleteUsers(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // S3 프로필 이미지 삭제
        String profile = user.getProfile();
        if (profile != null && !profile.isBlank()) {
            try {
                deleteFile(profile);
            } catch (Exception e) {
                // 이미지 삭제 실패해도 회원탈퇴는 진행
            }
        }

        userRepository.delete(user);
    }

    // 중복 확인
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}
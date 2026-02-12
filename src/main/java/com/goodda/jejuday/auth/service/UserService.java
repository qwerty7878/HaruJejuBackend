package com.goodda.jejuday.auth.service;

import com.goodda.jejuday.auth.dto.login.response.LoginResponse;
import com.goodda.jejuday.auth.entity.Gender;
import com.goodda.jejuday.auth.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Set;

public interface UserService {

    // 사용자 조회
    User getUserByEmail(String email);
    User getUserByEmailOrNull(String email);
    User getUserById(Long userId);
    Optional<User> findByUsername(String username);
    Long getAuthenticatedUserId();

    // 로그인 관련
    void setLoginCookie(HttpServletResponse response, String email);
    boolean matchesPassword(String rawPassword, String encodedPassword);
    LoginResponse loginResponse(User user);

    // 회원가입
    User createUser(String email, String password, String nickname, String profile,
                    Set<String> themeNames, Gender gender, String birthYear,
                    String referrerNickname);

    // 비밀번호 관련
    void resetPassword(String email, String newPassword);
    String generateVerificationCode();

    // 프로필 이미지
    String uploadProfileImage(MultipartFile profileImage);
    String getProfileImageUrl(Long userId);
    void updateUserProfileImage(Long userId, String newProfileUrl);
    void deleteFile(String fileUrl);

    // 회원 정보 업데이트
    void updateNickname(Long userId, String newNickname);
    void updateUserThemes(Long userId, Set<String> themeNames);

    // 알림 관련
    void updateFcmToken(Long userId, String fcmToken);
    void updateNotificationSetting(Long userId, boolean enabled);

    // 로그아웃 & 회원탈퇴
    void logoutUser(Long userId, HttpServletResponse response);
    void deleteUsers(String email);

    // 중복 확인
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
package com.goodda.jejuday.notification.controller;

import com.goodda.jejuday.auth.dto.ApiResponse;
import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.auth.security.CustomUserDetails;
import com.goodda.jejuday.auth.service.UserService;
import com.goodda.jejuday.notification.dto.FcmTokenUpdateRequest;
import com.goodda.jejuday.notification.dto.NotificationDto;
import com.goodda.jejuday.notification.dto.NotificationSettingRequest;
import com.goodda.jejuday.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/notification")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();
            return userService.getUserByEmail(email);
        }
        throw new IllegalArgumentException("인증된 유저가 아닙니다.");
    }

    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "사용자 본인의 알림 리스트를 가져옵니다.")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getMyNotifications() {
        User user = getAuthenticatedUser();
        List<NotificationDto> notifications = notificationService.getNotifications(user);
        return ResponseEntity.ok(ApiResponse.onSuccess(notifications));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리", description = "개별 알림을 읽음 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.onSuccess("알림이 읽음 처리되었습니다."));
    }

    @PostMapping("/fcm-token")
    @Operation(summary = "FCM 토큰 등록/수정", description = "로그인 후 클라이언트에서 전달된 FCM 토큰을 저장합니다.")
    public ResponseEntity<ApiResponse<String>> updateFcmToken(@RequestBody FcmTokenUpdateRequest request) {
        User user = getAuthenticatedUser();
        userService.updateFcmToken(user.getId(), request.getFcmToken());
        return ResponseEntity.ok(ApiResponse.onSuccess("FCM 토큰 업데이트 성공"));
    }

    @PostMapping("/notification-setting")
    @Operation(summary = "알림 설정 변경", description = "푸시 알림 수신 여부를 설정합니다.")
    public ResponseEntity<ApiResponse<String>> updateNotificationSetting(
            @RequestBody NotificationSettingRequest request) {
        User user = getAuthenticatedUser();
        userService.updateNotificationSetting(user.getId(), request.isEnabled());
        return ResponseEntity.ok(ApiResponse.onSuccess("알림 설정이 변경되었습니다."));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "단일 알림 삭제")
    public ResponseEntity<ApiResponse<String>> deleteNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        User user = userService.getUserById(userDetails.getUserId());
        notificationService.deleteOne(user, notificationId);
        return ResponseEntity.ok(ApiResponse.onSuccess("알림이 삭제되었습니다."));
    }

    @DeleteMapping("/all")
    @Operation(summary = "전체 알림 삭제")
    public ResponseEntity<ApiResponse<String>> deleteAllNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.getUserById(userDetails.getUserId());
        notificationService.deleteAll(user);
        return ResponseEntity.ok(ApiResponse.onSuccess("전체 알림이 삭제되었습니다."));
    }
}
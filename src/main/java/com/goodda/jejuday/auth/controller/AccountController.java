package com.goodda.jejuday.auth.controller;

import com.goodda.jejuday.auth.dto.ApiResponse;
import com.goodda.jejuday.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account", description = "사용자 탈퇴 API")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/users/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자의 계정을 비활성화합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteUsers(@RequestParam String email) {
        userService.deleteUsers(email);
        return new ResponseEntity<>(ApiResponse.onSuccess("회원 탈퇴 처리가 완료되었습니다."), HttpStatus.OK);
    }

    @PostMapping("/nickname")
    @Operation(summary = "닉네임 변경", description = "사용자의 닉네임을 변경합니다.")
    public ResponseEntity<ApiResponse<String>> updateNickname(@RequestParam String nickname) {
        Long userId = userService.getAuthenticatedUserId();
        userService.updateNickname(userId, nickname);
        return ResponseEntity.ok(ApiResponse.onSuccess("닉네임이 변경되었습니다."));
    }

    @PostMapping("/themes")
    @Operation(summary = "테마 변경", description = "사용자의 테마를 변경합니다.")
    public ResponseEntity<ApiResponse<String>> updateThemes(@RequestBody Set<String> themes) {
        Long userId = userService.getAuthenticatedUserId();
        userService.updateUserThemes(userId, themes);
        return ResponseEntity.ok(ApiResponse.onSuccess("테마가 변경되었습니다."));
    }
}

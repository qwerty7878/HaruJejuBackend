package com.goodda.jejuday.attendance.controller;

import com.goodda.jejuday.attendance.dto.AttendanceResult;
import com.goodda.jejuday.attendance.dto.AttendanceResponse;
import com.goodda.jejuday.attendance.service.AttendanceService;
import com.goodda.jejuday.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check")
    public ResponseEntity<AttendanceResponse> checkAttendance(@AuthenticationPrincipal CustomUserDetails user) {
        AttendanceResult result = attendanceService.checkAttendance(user.getUserId());

        if (result.alreadyChecked()) {
            return ResponseEntity.ok(AttendanceResponse.alreadyChecked());
        }

        return ResponseEntity.ok(AttendanceResponse.success(
                result.consecutiveDays(),
                result.baseHallabong(),
                result.bonusHallabong(),
                result.totalHallabong()
        ));
    }
}
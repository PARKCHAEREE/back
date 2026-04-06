package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.UserResponse;
import com.solarwise.capstonebackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "사용자 정보 관련 API")
public class UserController {

    private final AuthService authService;

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보 조회")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        log.info("내 정보 조회: userId={}", userId);

        UserResponse response = authService.getUserInfo(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success(response, "사용자 정보 조회 성공"));
    }

}


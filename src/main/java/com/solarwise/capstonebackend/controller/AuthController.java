package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.*;
import com.solarwise.capstonebackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * - 사용자 회원가입, 로그인, 로그아웃 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "사용자 인증 관련 API")
public class AuthController {

    private final AuthService authService;

    /**
     * 사용자 회원가입
     */
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자 가입")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        UserResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다."));
    }

    /**
     * 사용자 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰 획득")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "로그인에 성공했습니다."));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "로그아웃", description = "현재 세션 종료")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        Long userId = parseUserId(authentication);

        log.info("로그아웃 요청: userId={}", userId);
        authService.logout(userId);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다."));
    }

    private Long parseUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return Long.parseLong(authentication.getPrincipal().toString());
    }

}


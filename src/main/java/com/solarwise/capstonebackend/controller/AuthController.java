package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.LoginRequest;
import com.solarwise.capstonebackend.dto.LoginResponse;
import com.solarwise.capstonebackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * - 사용자 로그인 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "사용자 인증 관련 API")
public class AuthController {

    private final AuthService authService;

    /**
     * 사용자 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰 획득")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

}


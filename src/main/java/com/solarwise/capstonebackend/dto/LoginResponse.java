package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken; // JWT 액세스 토큰
    private String refreshToken; // JWT 리프레시 토큰 (향후 추가)
    private UserResponse user; // 사용자 정보

}


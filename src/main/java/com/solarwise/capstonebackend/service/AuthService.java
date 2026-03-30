package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.LoginRequest;
import com.solarwise.capstonebackend.dto.LoginResponse;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.repository.UserRepository;
import com.solarwise.capstonebackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 인증 서비스
 * - 사용자 로그인 처리
 * - JWT 토큰 발급
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 로그인
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("비밀번호가 일치하지 않습니다.");
        }

        if (!user.getActive()) {
            throw new BusinessException("비활성화된 사용자입니다.");
        }

        String token = jwtUtil.generateToken(user.getId().toString());

        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .token(token)
                .build();
    }

}


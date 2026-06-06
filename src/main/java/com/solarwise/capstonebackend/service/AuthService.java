package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.LoginRequest;
import com.solarwise.capstonebackend.dto.LoginResponse;
import com.solarwise.capstonebackend.dto.SignupRequest;
import com.solarwise.capstonebackend.dto.UserResponse;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.repository.UserRepository;
import com.solarwise.capstonebackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 서비스
 * - 사용자 회원가입, 로그인 처리
 * - JWT 토큰 발급
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SimulationService simulationService;

    /**
     * 사용자 회원가입
     */
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("이미 가입된 이메일입니다.");
        }

        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .active(true)
                .createdAt(virtualNow)
                .updatedAt(virtualNow)
                .build();

        User savedUser = userRepository.save(user);
        log.info("사용자 회원가입: userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        return entityToUserResponse(savedUser);
    }

    /**
     * 사용자 로그인
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("비밀번호가 일치하지 않습니다.");
        }

        if (!user.getActive()) {
            throw new BusinessException("비활성화된 사용자입니다.");
        }

        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        user.setLastLoginAt(virtualNow);
        user.setUpdatedAt(virtualNow);
        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user.getId().toString(), user.getRole());

        log.info("사용자 로그인: userId={}, email={}", user.getId(), user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(null) // TODO: 리프레시 토큰 구현
                .user(entityToUserResponse(user))
                .build();
    }

    /**
     * 사용자 로그아웃 시각 기록
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        user.setLastLogoutAt(virtualNow);
        user.setUpdatedAt(virtualNow);
        userRepository.save(user);
    }

    /**
     * 사용자 정보 조회 (로그인 후)
     */
    @Transactional(readOnly = true)
    public UserResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        return entityToUserResponse(user);
    }

    /**
     * 엔티티를 UserResponse로 변환
     */
    private UserResponse entityToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

}

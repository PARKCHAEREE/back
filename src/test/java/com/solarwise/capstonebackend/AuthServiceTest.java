package com.solarwise.capstonebackend;

import com.solarwise.capstonebackend.dto.LoginRequest;
import com.solarwise.capstonebackend.dto.LoginResponse;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.repository.UserRepository;
import com.solarwise.capstonebackend.security.JwtUtil;
import com.solarwise.capstonebackend.service.AuthService;
import com.solarwise.capstonebackend.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SimulationService simulationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_updatesLastLoginAt() {
        User user = User.builder()
                .id(10L)
                .email("owner@solarwise.com")
                .password("encoded")
                .name("owner")
                .role("ADMIN")
                .active(true)
                .build();
        LocalDateTime virtualNow = LocalDateTime.of(2026, 5, 12, 14, 0);

        when(userRepository.findByEmail("owner@solarwise.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", "encoded")).thenReturn(true);
        when(simulationService.getVirtualCurrentTime()).thenReturn(virtualNow);
        when(jwtUtil.generateToken("10", "ADMIN")).thenReturn("token-abc");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.login(LoginRequest.builder()
                .email("owner@solarwise.com")
                .password("raw-password")
                .build());

        assertThat(response.getAccessToken()).isEqualTo("token-abc");
        assertThat(user.getLastLoginAt()).isEqualTo(virtualNow);
        assertThat(user.getUpdatedAt()).isEqualTo(virtualNow);
        verify(userRepository).save(user);
    }

    @Test
    void logout_updatesLastLogoutAt() {
        User user = User.builder().id(10L).email("owner@solarwise.com").build();
        LocalDateTime virtualNow = LocalDateTime.of(2026, 5, 12, 15, 0);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(simulationService.getVirtualCurrentTime()).thenReturn(virtualNow);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.logout(10L);

        assertThat(user.getLastLogoutAt()).isEqualTo(virtualNow);
        assertThat(user.getUpdatedAt()).isEqualTo(virtualNow);
        verify(userRepository).save(user);
    }

    @Test
    void logout_throwsWhenUserMissing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}


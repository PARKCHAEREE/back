package com.solarwise.capstonebackend.config;

import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitConfig {

    private final UserRepository userRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Transactional
    public CommandLineRunner initData() {
        return args -> {
            //  테스트 관리자 계정이 자동으로 생성
            if (userRepository.findByEmail("admin@solarwise.com").isEmpty()) {
                User admin = User.builder()
                        .email("admin@solarwise.com")
                        .password(passwordEncoder.encode("password123"))
                        .name("테스트관리자")
                        .role("MANAGER")
                        .active(true)
                        .build();
                userRepository.save(admin);
                log.info(" 테스트 관리자 계정 자동 생성 완료 (admin@solarwise.com / password123)");

                // 관리자 계정에 연결된 1번 발전소 자동으로 생성
                if (powerPlantRepository.findById(1L).isEmpty()) {
                    PowerPlant plant = PowerPlant.builder()
                            .name("전북 익산 1호 태양광")
                            .location("전북특별자치도 익산시")
                            .capacity(100.0)
                            .panelCount(500)
                            .nx(59)   // (기상청 서울 좌표)
                            .ny(94)
                            .user(admin)
                            .active(true)
                            .build();
                    powerPlantRepository.save(plant);
                    log.info(" 1번 테스트 발전소 자동 생성 완료");
                }
            }
        };
    }
}
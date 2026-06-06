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

import java.time.LocalDateTime;

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
            LocalDateTime now = LocalDateTime.now();
            if (userRepository.findByEmail("sass090023@gmail.com").isEmpty()) {
                User admin = User.builder()
                        .email("sass090023@gmail.com")
                        .password(passwordEncoder.encode("password123"))
                        .name("테스트관리자")
                        .role("ADMIN")
                        .active(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                userRepository.save(admin);
                log.info("테스트 관리자 계정 자동 생성 완료 (sass090023@gmail.com / password123)");

                if (powerPlantRepository.findBySiteId("KR10025001").isEmpty()) {
                    PowerPlant plant = PowerPlant.builder()
                            .name("장항 태양광 발전소")
                            .description("충남 서천군 장항읍 장항산단7길 93 태양광 발전소")
                            .location("충남 서천군 장항읍")
                            .capacity(10850.125)
                            .panelCount(11)
                            .inverterModel("INV-10000")
                            .latitude(36.0372673)
                            .longitude(126.6898854)
                            .kmaGridNx(56)
                            .kmaGridNy(65)
                            .siteId("KR10025001")
                            .user(admin)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    powerPlantRepository.save(plant);
                    log.info("장항 태양광 발전소 자동 생성 완료 (KR10025001, 10,850.125kW)");
                }
            }
        };
    }
}

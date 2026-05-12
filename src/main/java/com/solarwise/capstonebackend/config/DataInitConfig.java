package com.solarwise.capstonebackend.config;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.UserRepository;
import com.solarwise.capstonebackend.service.SimulationService;
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
    private final SimulationService simulationService;

    @Bean
    @Transactional
    public CommandLineRunner initData() {
        return args -> {
            LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
            // 백엔드 담당자(이승윤) 로컬 개발 및 Swagger 확인용 관리자 계정 자동 생성
            if (userRepository.findByEmail("admin@solarwise.com").isEmpty()) {
                User admin = User.builder()
                        .email("admin@solarwise.com")
                        .password(passwordEncoder.encode("password123"))
                        .name("테스트관리자")
                        .role("MANAGER")
                        .active(true)
                        .createdAt(virtualNow)
                        .updatedAt(virtualNow)
                        .build();
                userRepository.save(admin);
                log.info("테스트 관리자 계정 자동 생성 완료 (admin@solarwise.com / password123)");

                /*
                 * 실제 발전소 정보 (자문가 제공 데이터 기준)
                 * - 주소  : 충남 서천군 장항읍 장항산단7길 93
                 * - 설비  : 인버터 11대 × 986.375kW = 총 10,850.125kW (약 10.85MW)
                 * - GPS  : 위도 36.0372673, 경도 126.6898854
                 * - 기상청 격자(nx/ny) : 충남 서천군 기준 추정값 (nx=56, ny=65)
                 *   → KMA 격자 변환 도구(https://www.kma.go.kr) 에서 정확한 값 확인 후 업데이트 필요
                 * - 사이트 ID : KR10025001 (자문가 제공 CSV의 V_SITE_ID)
                 */
                if (powerPlantRepository.findBySiteId("KR10025001").isEmpty()) {
                    PowerPlant plant = PowerPlant.builder()
                            .name("장항 태양광 발전소")
                            .description("충남 서천군 장항읍 장항산단7길 93 태양광 발전소 (인버터 11대, 총 10,850.125kW)")
                            .location("충남 서천군 장항읍 장항산단7길 93")
                            .capacity(10850.125)
                            .panelCount(11)
                            .inverterModel("986.375kW 인버터 × 11대")
                            .latitude(36.0372673)
                            .longitude(126.6898854)
                            .kmaGridNx(56)   // TODO: KMA 격자 변환으로 정확한 값 확인 필요
                            .kmaGridNy(65)   // TODO: KMA 격자 변환으로 정확한 값 확인 필요
                            .siteId("KR10025001")
                            .user(admin)
                            .active(true)
                            .createdAt(virtualNow)
                            .updatedAt(virtualNow)
                            .build();
                    powerPlantRepository.save(plant);
                    log.info("장항 태양광 발전소 자동 생성 완료 (KR10025001, 10,850.125kW)");
                }
            }
        };
    }
}
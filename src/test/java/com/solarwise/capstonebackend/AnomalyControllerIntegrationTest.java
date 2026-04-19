package com.solarwise.capstonebackend;

import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.dto.UpdateAnomalyStatusRequest;
import com.solarwise.capstonebackend.dto.UpdateAnomalyStatusResponse;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.service.AnomalyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyServiceTest {

    @Mock
    private AnomalyRepository anomalyRepository;

    @Mock
    private PowerPlantRepository powerPlantRepository;

    @InjectMocks
    private AnomalyService anomalyService;

    private User owner;
    private PowerPlant plant;
    private Anomaly anomaly;

    @BeforeEach
    void setUp() {
        owner = User.builder().build();

        plant = PowerPlant.builder()
                .id(1L)
                .name("테스트 발전소")
                .user(owner)
                .build();

        anomaly = Anomaly.builder()
                .id(100L)
                .powerPlant(plant)
                .type("POWER")
                .summary("예상 대비 발전량 감소")
                .severity("HIGH")
                .cause("일사량 대비 출력 저하")
                .recommendedAction("패널 상태 점검")
                .xaiExplanation("출력 저하율이 높습니다.")
                .status("DETECTED")
                .detectedAt(LocalDateTime.of(2026, 4, 19, 10, 0))
                .build();
    }

    // --- 상세 조회 테스트 ---

    @Test
    void 이상_상세조회시_DETECTED상태는_OPEN으로_정규화된다() {
        when(powerPlantRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(plant));
        when(anomalyRepository.findByIdAndPowerPlantId(100L, 1L)).thenReturn(Optional.of(anomaly));

        AnomalyDto result = anomalyService.getAnomalyDetail(1L, 100L, 1L);

        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getSummary()).isEqualTo("예상 대비 발전량 감소");
        assertThat(result.getCause()).isEqualTo("일사량 대비 출력 저하");
    }

    @Test
    void 이상_상세조회시_발전소_소유권_없으면_ResourceNotFoundException_발생() {
        when(powerPlantRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyService.getAnomalyDetail(1L, 100L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("발전소를 찾을 수 없습니다");
    }

    @Test
    void 이상_상세조회시_이벤트가_없으면_ResourceNotFoundException_발생() {
        when(powerPlantRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(plant));
        when(anomalyRepository.findByIdAndPowerPlantId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyService.getAnomalyDetail(1L, 999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("이상 이벤트를 찾을 수 없습니다");
    }

    // --- 상태 변경 테스트 ---

    @Test
    void 이상_상태변경_RESOLVED로_변경시_resolvedAt이_설정된다() {
        when(powerPlantRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(plant));
        when(anomalyRepository.findByIdAndPowerPlantId(100L, 1L)).thenReturn(Optional.of(anomaly));
        when(anomalyRepository.save(any(Anomaly.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAnomalyStatusResponse result = anomalyService.updateAnomalyStatus(1L, 100L, 1L, "RESOLVED");

        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(anomaly.getResolvedAt()).isNotNull();
    }

    @Test
    void 이상_상태변경_ACKNOWLEDGED로_변경시_resolvedAt이_초기화된다() {
        anomaly.setResolvedAt(LocalDateTime.now()); // 미리 설정
        when(powerPlantRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(plant));
        when(anomalyRepository.findByIdAndPowerPlantId(100L, 1L)).thenReturn(Optional.of(anomaly));
        when(anomalyRepository.save(any(Anomaly.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAnomalyStatusResponse result = anomalyService.updateAnomalyStatus(1L, 100L, 1L, "ACKNOWLEDGED");

        assertThat(result.getStatus()).isEqualTo("ACKNOWLEDGED");
        assertThat(anomaly.getResolvedAt()).isNull();
    }

    @Test
    void 이상_상태변경시_OPEN으로_되돌릴_수_있다() {
        anomaly.setStatus("RESOLVED");
        anomaly.setResolvedAt(LocalDateTime.now());
        when(powerPlantRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(plant));
        when(anomalyRepository.findByIdAndPowerPlantId(100L, 1L)).thenReturn(Optional.of(anomaly));
        when(anomalyRepository.save(any(Anomaly.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAnomalyStatusResponse result = anomalyService.updateAnomalyStatus(1L, 100L, 1L, "OPEN");

        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(anomaly.getResolvedAt()).isNull();
    }

    // --- 목록 조회 테스트 ---

    @Test
    void 이상_목록조회시_상태_정규화가_적용된다() {
        Anomaly legacyAnomaly = Anomaly.builder()
                .id(200L)
                .powerPlant(plant)
                .type("VISION")
                .summary("패널 오염")
                .severity("MEDIUM")
                .status("DETECTED")
                .detectedAt(LocalDateTime.now())
                .build();

        when(powerPlantRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(plant));
        when(anomalyRepository.findByPowerPlantIdOrderByDetectedAtDesc(1L)).thenReturn(List.of(legacyAnomaly));

        List<AnomalyDto> results = anomalyService.getRecentAnomalies(1L, 1L, 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo("OPEN");
    }
}

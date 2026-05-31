package com.solarwise.capstonebackend;

import com.solarwise.capstonebackend.dto.DashboardTimelineResponse;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.service.AnomalyService;
import com.solarwise.capstonebackend.service.DashboardService;
import com.solarwise.capstonebackend.service.EnergyAggregationService;
import com.solarwise.capstonebackend.service.SimulationService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PowerPlantRepository powerPlantRepository;

    @Mock
    private EnergyAggregationService energyAggregationService;

    @Mock
    private AnomalyService anomalyService;

    @Mock
    private SimulationService simulationService;

    @Mock
    private AnomalyRepository anomalyRepository;

    @Mock
    private PlantFeatureLogRepository plantFeatureLogRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private PowerPlant plant;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .id(10L)
                .email("owner@solarwise.com")
                .password("encoded-password")
                .name("owner")
                .build();

        plant = PowerPlant.builder()
                .id(1L)
                .name("demo-plant")
                .location("seoul")
                .capacity(100.0)
                .panelCount(20)
                .user(owner)
                .build();
    }

    @Test
    void getDashboardTimeline_buildsSeriesAndMarkers_usingDefaultDayRange() {
        LocalDateTime virtualNow = LocalDateTime.of(2026, 3, 20, 12, 0);
        LocalDateTime windowStart = virtualNow.minusHours(24);
        LocalDateTime forecastEnd = virtualNow.plusHours(24);

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(simulationService.getVirtualCurrentTime()).thenReturn(virtualNow);

        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                1L, windowStart, virtualNow)).thenReturn(List.of(
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(LocalDateTime.of(2026, 3, 20, 11, 0))
                        .actual(90.0)
                        .prediction(100.0)
                        .build(),
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(LocalDateTime.of(2026, 3, 20, 12, 0))
                        .actual(100.0)
                        .prediction(100.0)
                        .build()
        ));

        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                1L, windowStart, forecastEnd)).thenReturn(List.of(
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(LocalDateTime.of(2026, 3, 20, 11, 0))
                        .prediction(100.0)
                        .build(),
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(LocalDateTime.of(2026, 3, 20, 13, 0))
                        .prediction(105.0)
                        .build()
        ));

        when(anomalyRepository.findByPowerPlantIdAndDetectedAtBetweenOrderByDetectedAtAsc(
                1L, windowStart, virtualNow)).thenReturn(List.of(
                Anomaly.builder()
                        .id(99L)
                        .type("POWER")
                        .severity("HIGH")
                        .status("OPEN")
                        .summary("괴리 초과")
                        .detectedAt(LocalDateTime.of(2026, 3, 20, 11, 0))
                        .build()
        ));

        DashboardTimelineResponse response = dashboardService.getDashboardTimeline(1L, 10L, "DAY", null, null);

        assertThat(response.getPlantId()).isEqualTo(1L);
        assertThat(response.getRange()).isEqualTo("DAY");
        assertThat(response.getWindowStart()).isEqualTo(windowStart);
        assertThat(response.getWindowEnd()).isEqualTo(virtualNow);
        assertThat(response.getForecastEnd()).isEqualTo(forecastEnd);

        assertThat(response.getActualSeries()).hasSize(2);
        assertThat(response.getPredictionSeries()).hasSize(2);
        assertThat(response.getGapSeries()).hasSize(2);
        assertThat(response.getGapSeries().get(0).getAbsGap()).isEqualTo(10.0);
        assertThat(response.getGapSeries().get(0).getGapRate()).isEqualTo(0.1);

        assertThat(response.getAnomalyMarkers()).hasSize(1);
        assertThat(response.getAnomalyMarkers().get(0).getEventId()).isEqualTo(99L);
    }

    @Test
    void getDashboardTimeline_throwsWhenOwnershipMissing() {
        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getDashboardTimeline(1L, 10L, "DAY", null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("발전소를 찾을 수 없습니다");
    }

    @Test
    void getDashboardTimeline_fallsBackToDayRangeOnInvalidInput() {
        LocalDateTime to = LocalDateTime.of(2026, 3, 22, 9, 0);
        LocalDateTime expectedStart = to.minusHours(24);
        LocalDateTime expectedForecastEnd = to.plusHours(24);

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(simulationService.getVirtualCurrentTime()).thenReturn(LocalDateTime.of(2026, 3, 20, 12, 0));
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                eq(1L), eq(expectedStart), eq(to))).thenReturn(List.of());
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                eq(1L), eq(expectedStart), eq(expectedForecastEnd))).thenReturn(List.of());
        when(anomalyRepository.findByPowerPlantIdAndDetectedAtBetweenOrderByDetectedAtAsc(
                eq(1L), eq(expectedStart), eq(to))).thenReturn(List.of());

        DashboardTimelineResponse response = dashboardService.getDashboardTimeline(1L, 10L, "INVALID", null, to);

        assertThat(response.getRange()).isEqualTo("DAY");
        assertThat(response.getWindowStart()).isEqualTo(expectedStart);
        assertThat(response.getWindowEnd()).isEqualTo(to);
        assertThat(response.getForecastEnd()).isEqualTo(expectedForecastEnd);
    }
}



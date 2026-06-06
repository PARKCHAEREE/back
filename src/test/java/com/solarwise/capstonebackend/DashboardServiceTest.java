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
import org.springframework.data.domain.PageRequest;
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
        LocalDateTime dataStart = LocalDateTime.of(2025, 9, 26, 0, 0);
        LocalDateTime dataEnd = LocalDateTime.of(2026, 2, 23, 23, 0);
        LocalDateTime windowStart = virtualNow.minusHours(24);
        LocalDateTime forecastEnd = virtualNow.plusHours(72);

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(simulationService.getVirtualCurrentTime()).thenReturn(virtualNow);
        when(plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtAsc(1L))
                .thenReturn(PlantFeatureLog.builder().measuredAt(dataStart).build());
        when(plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(1L))
                .thenReturn(PlantFeatureLog.builder().measuredAt(dataEnd).build());

        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                1L, dataEnd.minusHours(24), dataEnd)).thenReturn(List.of(
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
                1L, dataEnd.minusHours(24), dataEnd)).thenReturn(List.of(
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
                1L, dataEnd.minusHours(24), dataEnd)).thenReturn(List.of(
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
        assertThat(response.getWindowStart()).isEqualTo(dataEnd.minusHours(24));
        assertThat(response.getWindowEnd()).isEqualTo(dataEnd);
        assertThat(response.getForecastEnd()).isEqualTo(dataEnd);

        assertThat(response.getActualSeries()).hasSize(2);
        assertThat(response.getPredictionSeries()).hasSize(2);
        assertThat(response.getGapSeries()).hasSize(2);
        assertThat(response.getGapSeries().get(0).getAbsoluteGap()).isEqualTo(10.0);
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
        LocalDateTime dataStart = LocalDateTime.of(2025, 9, 26, 0, 0);
        LocalDateTime dataEnd = LocalDateTime.of(2026, 2, 23, 23, 0);
        LocalDateTime expectedStart = dataEnd.minusHours(24);
        LocalDateTime expectedForecastEnd = dataEnd;

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(simulationService.getVirtualCurrentTime()).thenReturn(LocalDateTime.of(2026, 3, 20, 12, 0));
        when(plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtAsc(1L))
                .thenReturn(PlantFeatureLog.builder().measuredAt(dataStart).build());
        when(plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(1L))
                .thenReturn(PlantFeatureLog.builder().measuredAt(dataEnd).build());
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                eq(1L), eq(expectedStart), eq(dataEnd))).thenReturn(List.of());
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                eq(1L), eq(expectedStart), eq(expectedForecastEnd))).thenReturn(List.of());
        when(anomalyRepository.findByPowerPlantIdAndDetectedAtBetweenOrderByDetectedAtAsc(
                eq(1L), eq(expectedStart), eq(dataEnd))).thenReturn(List.of());

        DashboardTimelineResponse response = dashboardService.getDashboardTimeline(1L, 10L, "INVALID", null, to);

        assertThat(response.getRange()).isEqualTo("DAY");
        assertThat(response.getWindowStart()).isEqualTo(expectedStart);
        assertThat(response.getWindowEnd()).isEqualTo(dataEnd);
        assertThat(response.getForecastEnd()).isEqualTo(expectedForecastEnd);
    }

    @Test
    void getDashboardTimeline_usesCsvPredictionRowsThroughFutureWindow() {
        LocalDateTime virtualNow = LocalDateTime.of(2026, 3, 20, 12, 0);
        LocalDateTime dataStart = LocalDateTime.of(2025, 9, 26, 0, 0);
        LocalDateTime dataEnd = LocalDateTime.of(2026, 2, 23, 23, 0);
        LocalDateTime windowStart = dataEnd.minusWeeks(1);
        LocalDateTime forecastEnd = dataEnd;

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(simulationService.getVirtualCurrentTime()).thenReturn(virtualNow);
        when(plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtAsc(1L))
                .thenReturn(PlantFeatureLog.builder().measuredAt(dataStart).build());
        when(plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(1L))
                .thenReturn(PlantFeatureLog.builder().measuredAt(dataEnd).build());

        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                1L, windowStart, dataEnd)).thenReturn(List.of(
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(virtualNow.minusHours(1))
                        .actual(90.0)
                        .prediction(100.0)
                        .build()
        ));

        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                1L, windowStart, forecastEnd)).thenReturn(List.of(
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(virtualNow.minusHours(1))
                        .prediction(100.0)
                        .build(),
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(virtualNow.plusHours(6))
                        .prediction(120.0)
                        .build()
        ));

        when(anomalyRepository.findByPowerPlantIdAndDetectedAtBetweenOrderByDetectedAtAsc(
                1L, windowStart, dataEnd)).thenReturn(List.of());

        DashboardTimelineResponse response = dashboardService.getDashboardTimeline(1L, 10L, "WEEK", 6, null);

        assertThat(response.getRange()).isEqualTo("WEEK");
        assertThat(response.getWindowStart()).isEqualTo(windowStart);
        assertThat(response.getWindowEnd()).isEqualTo(dataEnd);
        assertThat(response.getForecastEnd()).isEqualTo(forecastEnd);
        assertThat(response.getActualSeries()).hasSize(1);
        assertThat(response.getActualSeries().get(0).getMeasuredAt()).isBeforeOrEqualTo(virtualNow);
        assertThat(response.getPredictionSeries()).hasSize(2);
        assertThat(response.getPredictionSeries().get(1).getMeasuredAt()).isEqualTo(virtualNow.plusHours(6));
        assertThat(response.getPredictionSeries().get(1).getPowerKw()).isEqualTo(120.0);
    }

    @Test
    void getDashboardTimeline_adjustsWindowToLatestData_whenRequestedWindowHasNoRows() {
        LocalDateTime virtualNow = LocalDateTime.of(2026, 3, 20, 12, 0);
        LocalDateTime dataStart = LocalDateTime.of(2025, 9, 26, 0, 0);
        LocalDateTime requestedStart = virtualNow.minusDays(1);
        LocalDateTime latestDataTime = LocalDateTime.of(2026, 2, 23, 23, 0);
        LocalDateTime adjustedStart = latestDataTime.minusDays(1);
        LocalDateTime adjustedForecastEnd = latestDataTime.plusHours(72);

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(simulationService.getVirtualCurrentTime()).thenReturn(virtualNow);
        when(plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtAsc(1L))
                .thenReturn(PlantFeatureLog.builder().measuredAt(dataStart).build());
        when(plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(1L))
                .thenReturn(PlantFeatureLog.builder().measuredAt(latestDataTime).build());

        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                1L, requestedStart, virtualNow)).thenReturn(List.of());
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(
                1L, virtualNow, PageRequest.of(0, 1))).thenReturn(List.of(
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(latestDataTime)
                        .actual(95.0)
                        .prediction(100.0)
                        .build()
        ));
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                1L, adjustedStart, latestDataTime)).thenReturn(List.of(
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(latestDataTime)
                        .actual(95.0)
                        .prediction(100.0)
                        .build()
        ));
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                1L, adjustedStart, adjustedForecastEnd)).thenReturn(List.of(
                PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(latestDataTime)
                        .prediction(100.0)
                        .build()
        ));
        when(anomalyRepository.findByPowerPlantIdAndDetectedAtBetweenOrderByDetectedAtAsc(
                1L, adjustedStart, latestDataTime)).thenReturn(List.of());

        DashboardTimelineResponse response = dashboardService.getDashboardTimeline(1L, 10L, "DAY", null, null);

        assertThat(response.getWindowEnd()).isEqualTo(latestDataTime);
        assertThat(response.getWindowStart()).isEqualTo(adjustedStart);
        assertThat(response.getForecastEnd()).isEqualTo(adjustedForecastEnd);
        assertThat(response.getActualSeries()).hasSize(1);
        assertThat(response.getPredictionSeries()).hasSize(1);
        assertThat(response.getGapSeries()).hasSize(1);
    }
}



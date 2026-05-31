package com.solarwise.capstonebackend;

import com.solarwise.capstonebackend.dto.PowerAnomalyTriggerRequest;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.VisionAnalysisRepository;
import com.solarwise.capstonebackend.service.NotificationService;
import com.solarwise.capstonebackend.service.SimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock
    private PowerPlantRepository powerPlantRepository;

    @Mock
    private AnomalyRepository anomalyRepository;

    @Mock
    private PlantFeatureLogRepository plantFeatureLogRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VisionAnalysisRepository visionAnalysisRepository;

    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        simulationService = new SimulationService(
                powerPlantRepository,
                anomalyRepository,
                plantFeatureLogRepository,
                notificationService,
                visionAnalysisRepository
        );
    }

    @Test
    void playbackStartAndStop_updatesRunningStatus() {
        assertThat(simulationService.isPlaybackRunning()).isFalse();

        simulationService.startPlayback();
        assertThat(simulationService.isPlaybackRunning()).isTrue();

        simulationService.stopPlayback();
        assertThat(simulationService.isPlaybackRunning()).isFalse();
    }

    @Test
    void simulateTimeProgression_doesNotAdvanceWhenPlaybackStopped() {
        LocalDateTime before = simulationService.getVirtualCurrentTime();

        simulationService.simulateTimeProgression();

        assertThat(simulationService.getVirtualCurrentTime()).isEqualTo(before);
        assertThat(simulationService.getLastTickAt()).isNull();
    }

    @Test
    void simulateTimeProgression_advancesOneHourWhenPlaybackRunning() {
        LocalDateTime before = simulationService.getVirtualCurrentTime();
        simulationService.startPlayback();

        simulationService.simulateTimeProgression();

        LocalDateTime after = simulationService.getVirtualCurrentTime();
        assertThat(after).isEqualTo(before.plusHours(1));
        assertThat(simulationService.getLastTickAt()).isEqualTo(after);
    }

    @Test
    void triggerPowerAnomaly_adjustsFutureDataAndSetsFutureDetectedAt() {
        PowerPlant plant = PowerPlant.builder().id(1L).name("demo").build();
        when(powerPlantRepository.findById(1L)).thenReturn(Optional.of(plant));

        LocalDateTime anomalyStart = LocalDateTime.of(2026, 3, 15, 18, 0);
        LocalDateTime anomalyEnd = LocalDateTime.of(2026, 3, 15, 19, 0);
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                1L, anomalyStart, anomalyEnd))
                .thenReturn(List.of(
                        PlantFeatureLog.builder()
                                .powerPlantId(1L)
                                .measuredAt(anomalyStart)
                                .prediction(100.0)
                                .actual(100.0)
                                .build(),
                        PlantFeatureLog.builder()
                                .powerPlantId(1L)
                                .measuredAt(anomalyEnd)
                                .prediction(80.0)
                                .actual(80.0)
                                .build()
                ));
        when(plantFeatureLogRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(anomalyRepository.save(any(Anomaly.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PowerAnomalyTriggerRequest request = PowerAnomalyTriggerRequest.builder()
                .plantId(1L)
                .anomalySeverity("HIGH")
                .differencePercentage(40.0)
                .durationHours(2)
                .description("demo")
                .build();

        Anomaly result = simulationService.triggerPowerAnomaly(request);

        verify(plantFeatureLogRepository).saveAll(any());
        assertThat(result.getDetectedAt()).isEqualTo(anomalyStart);
        assertThat(result.getCause()).contains("적용 건수 2");
    }

    @Test
    void simulateTimeProgression_doesNotAutoCreatePowerAnomaly() {
        simulationService.startPlayback();
        simulationService.simulateTimeProgression();

        verify(anomalyRepository, never()).existsByPowerPlantIdAndTypeAndStatus(any(), any(), any());
        verify(anomalyRepository, never()).save(any(Anomaly.class));
    }
}





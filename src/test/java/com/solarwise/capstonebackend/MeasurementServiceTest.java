package com.solarwise.capstonebackend;

import com.solarwise.capstonebackend.dto.DashboardSummaryDto;
import com.solarwise.capstonebackend.dto.MeasurementCsvUploadResult;
import com.solarwise.capstonebackend.dto.MeasurementSeriesDto;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.service.MeasurementService;
import com.solarwise.capstonebackend.service.SimulationService;
import com.solarwise.capstonebackend.util.CsvParsingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {

    @Mock
    private PlantFeatureLogRepository plantFeatureLogRepository;

    @Mock
    private PowerPlantRepository powerPlantRepository;

    @Mock
    private AnomalyRepository anomalyRepository;

    @Mock
    private CsvParsingUtil csvParsingUtil;

    @Mock
    private SimulationService simulationService;

    @InjectMocks
    private MeasurementService measurementService;

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
                .name("seocheon-plant")
                .location("seocheon")
                .capacity(10850.125)
                .panelCount(11)
                .user(owner)
                .build();
    }

    @Test
    void uploadMeasurementCsv_savesValidRowsAndSkipsDuplicatesAndInvalidRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "measurements.csv", "text/csv", "dummy".getBytes()
        );

        List<String[]> csvRows = List.of(
                new String[]{"TIME", "D_PERIOD_GEN_KWH", "D_TEMP", "D_HUMIDITY"},
                new String[]{"2026042600", "10.5", "18.0", "70.0"},
                new String[]{"2026042601", "11.0", "", "71.0"},
                new String[]{"invalid", "12.0", "20.0", "72.0"},
                new String[]{"2026042600", "10.5", "18.0", "70.0"},
                new String[]{"2026042602", "13.0", "21.0", "73.0"}
        );

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(csvParsingUtil.parseCsv(file)).thenReturn(csvRows);
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                eq(1L), eq(LocalDateTime.of(2026, 4, 26, 0, 0)), eq(LocalDateTime.of(2026, 4, 26, 2, 0))))
                .thenReturn(List.of(PlantFeatureLog.builder()
                        .powerPlantId(1L)
                        .measuredAt(LocalDateTime.of(2026, 4, 26, 2, 0))
                        .actual(13.0)
                        .temp(21.0)
                        .humi(73.0)
                        .build()));
        when(plantFeatureLogRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MeasurementCsvUploadResult result = measurementService.uploadMeasurementCsv(1L, 10L, file);

        assertThat(result.getTotalRows()).isEqualTo(5);
        assertThat(result.getParsedRows()).isEqualTo(4);
        assertThat(result.getSavedRows()).isEqualTo(2);
        assertThat(result.getDuplicateRows()).isEqualTo(2);
        assertThat(result.getSkippedRows()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PlantFeatureLog>> captor = ArgumentCaptor.forClass((Class<List<PlantFeatureLog>>) (Class<?>) List.class);
        verify(plantFeatureLogRepository).saveAll(captor.capture());
        List<PlantFeatureLog> savedLogs = captor.getValue();

        assertThat(savedLogs).hasSize(2);
        assertThat(savedLogs.get(0).getMeasuredAt()).isEqualTo(LocalDateTime.of(2026, 4, 26, 0, 0));
        assertThat(savedLogs.get(0).getActual()).isEqualTo(10.5);
        assertThat(savedLogs.get(1).getMeasuredAt()).isEqualTo(LocalDateTime.of(2026, 4, 26, 1, 0));
        assertThat(savedLogs.get(1).getTemp()).isEqualTo(18.0);
        assertThat(savedLogs.get(1).getHumi()).isEqualTo(71.0);
    }

    @Test
    void uploadMeasurementCsv_throwsWhenPlantOwnershipIsMissing() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "measurements.csv", "text/csv", "dummy".getBytes()
        );

        when(powerPlantRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> measurementService.uploadMeasurementCsv(1L, 99L, file))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("발전소를 찾을 수 없습니다");
    }

    @Test
    void getDashboardSummary_usesActualForTodayGeneration() {
        PlantFeatureLog latestLog = PlantFeatureLog.builder()
                .powerPlantId(1L)
                .measuredAt(LocalDateTime.of(2026, 5, 2, 14, 0))
                .actual(20.0)
                .temp(24.0)
                .humi(55.0)
                .build();

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(1L)).thenReturn(latestLog);
        when(simulationService.getVirtualCurrentTime()).thenReturn(LocalDateTime.of(2026, 5, 2, 14, 30));
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(eq(1L), any(), any()))
                .thenReturn(List.of(
                        latestLog,
                        PlantFeatureLog.builder()
                                .powerPlantId(1L)
                                .measuredAt(LocalDateTime.of(2026, 5, 2, 13, 0))
                                .actual(15.0)
                                .build()
                ));
        when(anomalyRepository.findByPowerPlantIdOrderByDetectedAtDesc(1L)).thenReturn(List.of());

        DashboardSummaryDto result = measurementService.getDashboardSummary(1L, 10L);

        assertThat(result.getCurrentPowerKw()).isEqualTo(20.0);
        assertThat(result.getTodayGenerationKwh()).isEqualTo(35.0);
        assertThat(result.getLatestAnomaly().isExists()).isFalse();
    }

    @Test
    void getMeasurementSeries_returnsActualAndWeatherFields() {
        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                eq(1L), eq(LocalDateTime.of(2026, 5, 1, 0, 0)), eq(LocalDateTime.of(2026, 5, 1, 2, 0))))
                .thenReturn(List.of(
                        PlantFeatureLog.builder()
                                .powerPlantId(1L)
                                .measuredAt(LocalDateTime.of(2026, 5, 1, 0, 0))
                                .actual(8.0)
                                .temp(19.0)
                                .irradiance(0.0)
                                .humi(80.0)
                                .build(),
                        PlantFeatureLog.builder()
                                .powerPlantId(1L)
                                .measuredAt(LocalDateTime.of(2026, 5, 1, 1, 0))
                                .actual(12.5)
                                .temp(20.0)
                                .irradiance(0.0)
                                .humi(78.0)
                                .build()
                ));

        MeasurementSeriesDto result = measurementService.getMeasurementSeries(
                1L, 10L,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 1, 2, 0)
        );

        assertThat(result.getPlantId()).isEqualTo(1L);
        assertThat(result.getSeries()).hasSize(2);
        assertThat(result.getSeries().get(0).getEnergyKwh()).isEqualTo(8.0);
        assertThat(result.getSeries().get(1).getPowerKw()).isEqualTo(12.5);
    }
}

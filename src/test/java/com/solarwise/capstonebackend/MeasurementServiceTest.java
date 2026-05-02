package com.solarwise.capstonebackend;

import com.solarwise.capstonebackend.dto.DashboardSummaryDto;
import com.solarwise.capstonebackend.dto.MeasurementCsvUploadResult;
import com.solarwise.capstonebackend.dto.MeasurementSeriesDto;
import com.solarwise.capstonebackend.entity.EnergyLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.EnergyLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.service.MeasurementService;
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
    private EnergyLogRepository energyLogRepository;

    @Mock
    private PowerPlantRepository powerPlantRepository;

    @Mock
    private AnomalyRepository anomalyRepository;

    @Mock
    private CsvParsingUtil csvParsingUtil;

    @InjectMocks
    private MeasurementService measurementService;

    private PowerPlant plant;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .id(10L)
                .email("owner@solarwise.com")
                .password("encoded-password")
                .name("담당자")
                .build();

        plant = PowerPlant.builder()
                .id(1L)
                .name("서천 발전소")
                .location("충남 서천군")
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
                new String[]{"V_SITE_ID", "TIME", "D_PERIOD_GEN_KWH", "D_TEMP", "D_HUMIDITY"},
                new String[]{"KR10025001", "2026042600", "10.5", "18.0", "70.0"},
                new String[]{"KR10025001", "2026042601", "11.0", "", "71.0"},
                new String[]{"KR10025001", "invalid", "12.0", "20.0", "72.0"},
                new String[]{"KR10025001", "2026042600", "10.5", "18.0", "70.0"},
                new String[]{"KR10025001", "2026042602", "13.0", "21.0", "73.0"}
        );

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(csvParsingUtil.parseCsv(file)).thenReturn(csvRows);
        when(energyLogRepository.findByPowerPlantIdAndTimestampBetweenOrderByTimestampAsc(
                eq(1L), eq(LocalDateTime.of(2026, 4, 26, 0, 0)), eq(LocalDateTime.of(2026, 4, 26, 2, 0))))
                .thenReturn(List.of(EnergyLog.builder()
                        .powerPlant(plant)
                        .timestamp(LocalDateTime.of(2026, 4, 26, 2, 0))
                        .powerKw(13.0)
                        .energyKwh(13.0)
                        .temperature(21.0)
                        .irradiance(0.0)
                        .humidity(73.0)
                        .build()));
        when(energyLogRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MeasurementCsvUploadResult result = measurementService.uploadMeasurementCsv(1L, 10L, file);

        assertThat(result.getTotalRows()).isEqualTo(5);
        assertThat(result.getParsedRows()).isEqualTo(4);
        assertThat(result.getSavedRows()).isEqualTo(2);
        assertThat(result.getDuplicateRows()).isEqualTo(2);
        assertThat(result.getSkippedRows()).isEqualTo(1);
        assertThat(result.getFirstTimestamp()).isEqualTo(LocalDateTime.of(2026, 4, 26, 0, 0));
        assertThat(result.getLastTimestamp()).isEqualTo(LocalDateTime.of(2026, 4, 26, 2, 0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EnergyLog>> captor = ArgumentCaptor.forClass((Class<List<EnergyLog>>) (Class<?>) List.class);
        verify(energyLogRepository).saveAll(captor.capture());
        List<EnergyLog> savedLogs = captor.getValue();

        assertThat(savedLogs).hasSize(2);
        assertThat(savedLogs.getFirst().getTimestamp()).isEqualTo(LocalDateTime.of(2026, 4, 26, 0, 0));
        assertThat(savedLogs.getFirst().getPowerKw()).isEqualTo(10.5);
        assertThat(savedLogs.getFirst().getEnergyKwh()).isEqualTo(10.5);
        assertThat(savedLogs.get(1).getTimestamp()).isEqualTo(LocalDateTime.of(2026, 4, 26, 1, 0));
        assertThat(savedLogs.get(1).getTemperature()).isEqualTo(18.0); // 앞뒤 보간: (18.0 + 18.0) / 2 = 18.0
        assertThat(savedLogs.get(1).getHumidity()).isEqualTo(71.0);
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
    void getDashboardSummary_usesEnergyKwhForTodayGeneration() {
        EnergyLog latestLog = EnergyLog.builder()
                .powerPlant(plant)
                .timestamp(LocalDateTime.of(2026, 5, 2, 14, 0))
                .powerKw(20.0)
                .energyKwh(20.0)
                .temperature(24.0)
                .irradiance(0.0)
                .humidity(55.0)
                .build();

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(energyLogRepository.findTopByPowerPlantIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(latestLog));
        when(energyLogRepository.findByPowerPlantIdAndTimestampBetweenOrderByTimestampAsc(eq(1L), any(), any()))
                .thenReturn(List.of(
                        latestLog,
                        EnergyLog.builder()
                                .powerPlant(plant)
                                .timestamp(LocalDateTime.of(2026, 5, 2, 13, 0))
                                .powerKw(15.0)
                                .energyKwh(15.0)
                                .temperature(23.0)
                                .irradiance(0.0)
                                .humidity(58.0)
                                .build()
                ));
        when(anomalyRepository.findByPowerPlantIdOrderByDetectedAtDesc(1L)).thenReturn(List.of());

        DashboardSummaryDto result = measurementService.getDashboardSummary(1L, 10L);

        assertThat(result.getCurrentPowerKw()).isEqualTo(20.0);
        assertThat(result.getTodayGenerationKwh()).isEqualTo(35.0);
        assertThat(result.getLatestAnomaly().isExists()).isFalse();
    }

    @Test
    void getMeasurementSeries_returnsEnergyKwhTogether() {
        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(energyLogRepository.findByPowerPlantIdAndTimestampBetweenOrderByTimestampAsc(
                eq(1L), eq(LocalDateTime.of(2026, 5, 1, 0, 0)), eq(LocalDateTime.of(2026, 5, 1, 2, 0))))
                .thenReturn(List.of(
                        EnergyLog.builder()
                                .powerPlant(plant)
                                .timestamp(LocalDateTime.of(2026, 5, 1, 0, 0))
                                .powerKw(8.0)
                                .energyKwh(8.0)
                                .temperature(19.0)
                                .irradiance(0.0)
                                .humidity(80.0)
                                .build(),
                        EnergyLog.builder()
                                .powerPlant(plant)
                                .timestamp(LocalDateTime.of(2026, 5, 1, 1, 0))
                                .powerKw(12.5)
                                .energyKwh(12.5)
                                .temperature(20.0)
                                .irradiance(0.0)
                                .humidity(78.0)
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





package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.PlantFeatureLogDto;
import com.solarwise.capstonebackend.dto.PlantFeatureLogSeriesDto;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 발전소 고급 피처 로그 서비스
 *
 * <p>
 * 이 서비스는 AI 팀이 전처리한 통합 CSV 데이터(wooyang_merged_result.csv)의 7개 컬럼을
 * 단일 엔티티 {@link PlantFeatureLog}에서 관리합니다.
 * </p>
 *
 * <h3>PlantFeatureLog의 7개 컬럼 매핑</h3>
 * <ul>
 *   <li>TIME → measuredAt (측정 시각)</li>
 *   <li>ACTUAL → actual (실제 발전량, kWh)</li>
 *   <li>PREDICTION → prediction (AI 예측 발전량, kWh)</li>
 *   <li>TEMP → temp (기온, °C)</li>
 *   <li>HUMI → humi (습도, %)</li>
 *   <li>CLOU → clou (운량, 0.0~1.0)</li>
 *   <li>IRRADIANCE → irradiance (일사량, W/m²)</li>
 * </ul>
 *
 * <h3>가상 시간 시뮬레이션 원칙</h3>
 * <ul>
 *   <li>모든 시간 기준은 {@link SimulationService#getVirtualCurrentTime()}에서 얻어야 함</li>
 *   <li>LocalDateTime.now() 절대 금지</li>
 *   <li>DB의 CURRENT_TIMESTAMP 사용 금지</li>
 * </ul>
 *
 * @see PlantFeatureLog
 * @see SimulationService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantFeatureLogService {

    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final SimulationService simulationService;

    /**
     * 발전소별 피처 로그 전체 건수 조회
     *
     * @param plantId 발전소 ID
     * @param userId 사용자 ID (소유권 검증용)
     * @return 피처 로그 총 건수
     * @throws ResourceNotFoundException 발전소가 없거나 사용자 소유권 없음
     */
    public long getFeatureLogCount(Long plantId, Long userId) {
        assertPlantOwnership(plantId, userId);
        long count = plantFeatureLogRepository.countByPowerPlantId(plantId);
        log.debug("피처 로그 건수 조회: plantId={}, count={}", plantId, count);
        return count;
    }

    /**
     * 발전소의 최신 피처 로그 N건 조회 (가상 시간 기준)
     *
     * <p>
     * 이 메서드는 가상 시간 기준의 "최신" 데이터를 반환합니다.
     * 예를 들어, 가상 시간이 2026-04-26 18:00이면,
     * 그 이전의 최신 24건을 반환합니다.
     * </p>
     *
     * @param plantId 발전소 ID
     * @param userId 사용자 ID (소유권 검증용)
     * @param limit 조회할 최대 건수 (1~500)
     * @return 최신 N건의 피처 로그 시계열 DTO
     * @throws ResourceNotFoundException 발전소가 없거나 사용자 소유권 없음
     */
    public PlantFeatureLogSeriesDto getLatestFeatureLogs(Long plantId, Long userId, int limit) {
        assertPlantOwnership(plantId, userId);

        int safeLimit = Math.max(1, Math.min(limit, 500));

        // 🔑 가상 시간 기준: 현재 시간 이전의 최신 N건만 조회
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();

        List<PlantFeatureLogDto> latest = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(
                        plantId,
                        virtualNow,
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "measuredAt"))
                )
                .stream()
                .map(this::toDto)
                .toList();

        log.debug("최신 피처 로그 조회: plantId={}, virtualNow={}, returned={}",
                plantId, virtualNow, latest.size());

        return PlantFeatureLogSeriesDto.builder()
                .plantId(plantId)
                .series(latest)
                .build();
    }

    /**
     * 발전소의 기간별 피처 로그 조회 (가상 시간 기준)
     *
     * <p>
     * 주어진 기간[from, to] 내의 모든 피처 로그를 시간순으로 반환합니다.
     * 파라미터가 없으면 가상 시간 기준 과거 24시간을 조회합니다.
     * </p>
     *
     * @param plantId 발전소 ID
     * @param userId 사용자 ID (소유권 검증용)
     * @param from 조회 시작 시간 (없으면 to - 24h)
     * @param to 조회 종료 시간 (없으면 현재 가상 시간)
     * @return 기간별 피처 로그 시계열 DTO
     * @throws ResourceNotFoundException 발전소가 없거나 사용자 소유권 없음
     */
    public PlantFeatureLogSeriesDto getFeatureLogSeries(Long plantId, Long userId,
                                                         LocalDateTime from, LocalDateTime to) {
        assertPlantOwnership(plantId, userId);

        List<PlantFeatureLogDto> series = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, from, to)
                .stream()
                .map(this::toDto)
                .toList();

        log.debug("기간별 피처 로그 조회: plantId={}, from={}, to={}, returned={}",
                plantId, from, to, series.size());

        return PlantFeatureLogSeriesDto.builder()
                .plantId(plantId)
                .series(series)
                .build();
    }

    /**
     * 발전소 소유권 검증
     *
     * @param plantId 발전소 ID
     * @param userId 사용자 ID
     * @throws ResourceNotFoundException 발전소가 없거나 사용자 소유권 없음
     */
    private void assertPlantOwnership(Long plantId, Long userId) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        if (plant.getId() == null) {
            throw new ResourceNotFoundException("발전소를 찾을 수 없습니다.");
        }
    }

    /**
     * PlantFeatureLog 엔티티를 DTO로 변환
     *
     * <p>
     * CSV의 7개 컬럼(measuredAt, temp, humi, clou, wisp, irradiance, prediction, actual)을
     * 모두 DTO에 매핑합니다.
     * </p>
     *
     * @param log PlantFeatureLog 엔티티
     * @return PlantFeatureLogDto
     */
    private PlantFeatureLogDto toDto(PlantFeatureLog log) {
        return PlantFeatureLogDto.builder()
                .measuredAt(log.getMeasuredAt())
                .temp(log.getTemp())
                .humi(log.getHumi())
                .clou(log.getClou())
                .wisp(log.getWisp())
                .irradiance(log.getIrradiance())
                .prediction(log.getPrediction())
                .actual(log.getActual())
                .build();
    }
}

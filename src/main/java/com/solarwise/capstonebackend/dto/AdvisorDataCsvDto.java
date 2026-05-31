package com.solarwise.capstonebackend.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 우양 제공 태양광/기상 데이터 CSV 바인딩 DTO (최종 31개 피처 버전)
 * - OpenCSV의 @CsvBindByName을 사용하여 CSV 컬럼과 자동 매핑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorDataCsvDto {

    // 1. 기본 정보 및 타겟 변수
    @CsvBindByName(column = "TIME")
    private String time; // 시간 (예: "2024-01-01 00:00:00")

    @CsvBindByName(column = "ACTUAL")
    private Double actual; // 실제 발전량

    @CsvBindByName(column = "PREDICTION")
    private Double prediction; // 예측 발전량

    // 2. 기상/환경 변수
    @CsvBindByName(column = "TEMP")
    private Double temp; // 기온 (℃)

    @CsvBindByName(column = "HUMI")
    private Double humi; // 습도 (%)

    @CsvBindByName(column = "CLOU")
    private Double clou; // 구름량 (%)

    @CsvBindByName(column = "WISP")
    private Double wisp; // 풍속 (m/s)

    // 3. 시간 및 계절성 파생 변수 (태양 궤도 등)
    @CsvBindByName(column = "H_SIN")
    private Double hSin;

    @CsvBindByName(column = "H_COS")
    private Double hCos;

    @CsvBindByName(column = "DOY_SIN")
    private Double doySin;

    @CsvBindByName(column = "DOY_COS")
    private Double doyCos;

    @CsvBindByName(column = "WIDE_SIN")
    private Double wideSin;

    @CsvBindByName(column = "WIDE_COS")
    private Double wideCos;

    // 4. 일사량 및 발전소 용량 변수
    @CsvBindByName(column = "SUN_ELEV_CLIP")
    private Double sunElevClip;

    @CsvBindByName(column = "COS_ZEN")
    private Double cosZen;

    @CsvBindByName(column = "IRRADIANCE")
    private Double irradiance; // 일사량 (W/m²)

    @CsvBindByName(column = "EST_IRRADIANCE")
    private Double estIrradiance;

    @CsvBindByName(column = "IRRADIANCE_PROXY")
    private Double irradianceProxy;

    @CsvBindByName(column = "IRRADIANCE_X_CAPA")
    private Double irradianceXCapa;

    @CsvBindByName(column = "CAPA")
    private Double capa;

    // 5. 발전량 패턴 및 과거 시계열 (LAG)
    @CsvBindByName(column = "SEASONAL_SOLAR_PATTERN")
    private Double seasonalSolarPattern;

    @CsvBindByName(column = "WEATHER_ADJUSTED_PATTERN")
    private Double weatherAdjustedPattern;

    @CsvBindByName(column = "EXPECTED_GEN_PROXY")
    private Double expectedGenProxy;

    @CsvBindByName(column = "GEN_LAG_2")
    private Double genLag2; // GEN_LAG_1, GEN_ROLL_MEAN_6는 AI 팀에서 제외함

    // 6. 🚨 이상 탐지 (비전/XAI) 관련 신규 피처
    @CsvBindByName(column = "dust_coverage_ratio")
    private Double dustCoverageRatio;

    @CsvBindByName(column = "snow_coverage_ratio")
    private Double snowCoverageRatio;

    @CsvBindByName(column = "bird_dropping_count")
    private Integer birdDroppingCount; // 개수는 Integer로 처리

    @CsvBindByName(column = "physical_damage_count")
    private Integer physicalDamageCount; // 개수는 Integer로 처리

    @CsvBindByName(column = "max_defect_confidence")
    private Double maxDefectConfidence;

    @CsvBindByName(column = "cls_normal")
    private Integer clsNormal;

    @CsvBindByName(column = "cls_dust")
    private Integer clsDust;

    @CsvBindByName(column = "cls_snow")
    private Integer clsSnow;

    @CsvBindByName(column = "cls_bird")
    private Integer clsBird;

    @CsvBindByName(column = "cls_damage")
    private Integer clsDamage;
}
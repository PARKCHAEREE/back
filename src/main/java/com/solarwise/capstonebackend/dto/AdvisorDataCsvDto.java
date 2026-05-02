package com.solarwise.capstonebackend.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 우양 제공 태양광/기상 데이터 CSV 바인딩 DTO
 * - OpenCSV의 @CsvBindByName을 사용하여 CSV 컬럼과 자동 매핑
 * - 컬럼: V_TIME, D_PERIOD_GEN_KWH, D_TEMP, D_HUMIDITY, D_UVI, D_CLOUDS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorDataCsvDto {

    @CsvBindByName(column = "V_TIME")
    private String vTime; // 시간 (예: "2024-01-01 00:00:00")

    @CsvBindByName(column = "D_PERIOD_GEN_KWH")
    private Double dPeriodGenKwh; // 발전량 (kWh/시간)

    @CsvBindByName(column = "D_TEMP")
    private Double dTemp; // 기온 (℃)

    @CsvBindByName(column = "D_HUMIDITY")
    private Double dHumidity; // 습도 (%)

    @CsvBindByName(column = "D_UVI")
    private Double dUvi; // 자외선 지수 또는 일사량 (W/m² 또는 MJ/m²)

    @CsvBindByName(column = "D_CLOUDS")
    private Double dClouds; // 구름량 (%)

}


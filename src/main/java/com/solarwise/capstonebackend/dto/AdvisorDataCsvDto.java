package com.solarwise.capstonebackend.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 우양 제공 태양광/기상 데이터 CSV 바인딩 DTO
 * - OpenCSV의 @CsvBindByName을 사용하여 CSV 컬럼과 자동 매핑
 * - 컬럼: TIME, ACTUAL, PREDICTION, TEMP, HUMI, CLOU, IRRADIANCE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorDataCsvDto {

    @CsvBindByName(column = "TIME")
    private String time; // 시간 (예: "2024-01-01 00:00:00")

    @CsvBindByName(column = "ACTUAL")
    private Double actual; // 실제 발전량 (kWh/시간)

    @CsvBindByName(column = "PREDICTION")
    private Double prediction; // 예측 발전량 (kWh/시간)

    @CsvBindByName(column = "TEMP")
    private Double temp; // 기온 (℃)

    @CsvBindByName(column = "HUMI")
    private Double humi; // 습도 (%)

    @CsvBindByName(column = "CLOU")
    private Double clou; // 구름량 (%)

    @CsvBindByName(column = "IRRADIANCE")
    private Double irradiance; // 일사량 (W/m²)

}

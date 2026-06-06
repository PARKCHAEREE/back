package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 발전량과 예측량의 차이를 나타내는 DTO
 *
 * 용도: 대시보드 타임라인에서 같은 시간대의 CSV prediction 값과 actual 값을 비교한다.
 * 권장 판정 기준: 15% 이상 MEDIUM, 30% 이상 HIGH.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapDto {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime measuredAt;

    private Double prediction;

    private Double actual;

    private Double absoluteGap;  // |actual - prediction|

    private Double gapRate;      // |actual - prediction| / prediction (0.3 = 30%)

    /**
     * 생성자 Helper: 같은 measuredAt 행의 prediction, actual 기반으로 gap 자동 계산
     */
    public GapDto(LocalDateTime measuredAt, Double prediction, Double actual) {
        this.measuredAt = measuredAt;
        this.prediction = prediction;
        this.actual = actual;
        this.calculateGap();
    }

    /**
     * 차이 자동 계산
     */
    private void calculateGap() {
        if (prediction == null || actual == null) {
            this.absoluteGap = null;
            this.gapRate = null;
            return;
        }
        this.absoluteGap = Math.abs(actual - prediction);
        this.gapRate = prediction != 0 ? (Math.abs(actual - prediction) / prediction) : null;
    }
}

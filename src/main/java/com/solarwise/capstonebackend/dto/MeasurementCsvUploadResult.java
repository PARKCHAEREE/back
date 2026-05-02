package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 실측 CSV 업로드 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementCsvUploadResult {

    private Long plantId;

    private String fileName;

    private Integer totalRows;

    private Integer parsedRows;

    private Integer savedRows;

    private Integer duplicateRows;

    private Integer skippedRows;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime firstTimestamp;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastTimestamp;
}


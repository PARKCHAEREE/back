package com.solarwise.capstonebackend.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * CSV 파싱 유틸리티
 * - OpenCSV를 활용한 대용량 데이터 파싱
 */
@Slf4j
@Component
public class CsvParsingUtil {

    /**
     * CSV 파일 읽기
     */
    public List<String[]> readCsvFile(Reader reader) throws IOException, CsvException {
        try (CSVReader csvReader = new CSVReader(reader)) {
            return csvReader.readAll();
        } catch (IOException | CsvException e) {
            log.error("CSV 파일 읽기 실패: {}", e.getMessage());
            throw e;
        }
    }

}


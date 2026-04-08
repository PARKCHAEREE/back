package com.solarwise.capstonebackend.util;

import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class CsvParsingUtil {

    // CSV 파일을 읽어 List<String[]> 형태로 반환
    public List<String[]> parseCsv(MultipartFile file) {
        String encoding = "UTF-8";

        try (Reader reader = new InputStreamReader(file.getInputStream(), encoding);
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> records = csvReader.readAll();
            log.info("CSV 파싱 완료: 총 {}행", records.size());

            return records;

        } catch (Exception e) {
            log.error("CSV 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("CSV 처리 중 오류가 발생했습니다.", e);
        }
    }
}
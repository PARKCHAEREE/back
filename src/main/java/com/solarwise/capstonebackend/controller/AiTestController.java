package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.service.AiIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final AiIntegrationService aiIntegrationService;

    // 1.  발전량 예측 테스트 버튼
    @PostMapping("/predict/{plantId}")
    public ResponseEntity<String> testPowerPrediction(@PathVariable Long plantId) {
        aiIntegrationService.requestPredictionFromAi(plantId);
        return ResponseEntity.ok("AI 발전량 예측 요청이 백그라운드에서 시작되었습니다! (콘솔 로그를 확인하세요)");
    }

    //  2.  패널 결함(YOLO) 통신 테스트 버튼
    // 스웨거에서 이 버튼을 누르면 코랩의 /internal/anomaly/vision-detect 주소로 신호가 날아갑니다!
    @PostMapping("/vision/{plantId}")
    public ResponseEntity<String> testVisionDetection(@PathVariable Long plantId) {
        String dummyPanelId = "PANEL_001";

        // src/main/resources/images/crack.jpg 파일을 읽어서 Base64 텍스트로 변환함
        String realImageBase64 = aiIntegrationService.encodeImageToBase64("crack.jpg");

        aiIntegrationService.detectVisionAnomaly(plantId, dummyPanelId, realImageBase64);
        return ResponseEntity.ok("진짜 크랙 사진으로 YOLO 분석 요청을 보냈습니다!");
    }
}
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

        // 💡 테스트용 가짜 데이터(패널 ID와 더미 이미지 텍스트)를 넣어서 코랩을 찌릅니다.
        String dummyPanelId = "PANEL_001";
        String dummyBase64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="; // 1픽셀짜리 가짜 이미지 데이터

        // 비동기(@Async)로 패널 결함 분석 서비스를 호출합니다.
        aiIntegrationService.detectVisionAnomaly(plantId, dummyPanelId, dummyBase64Image);

        return ResponseEntity.ok("YOLO 패널 결함 분석 요청이 백그라운드에서 시작되었습니다! (코랩 창과 인텔리제이 콘솔 로그를 확인하세요)");
    }
}
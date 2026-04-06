# 대시보드 및 측정 데이터 API 구현 완료

## 개요
측정 데이터 및 대시보드 조회 API를 구현했습니다.
- 대시보드 요약 조회
- 시계열 측정 데이터 조회
- 이상 탐지 목록 조회 (명세 기준 재정의)

## 생성한 파일

### DTO (데이터 전송 객체)

#### `dto/MeasurementDto.java`
시계열 측정 데이터 포인트
```java
{
  "measuredAt": "2026-04-05T10:00:00Z",
  "powerKw": 81.2,
  "temperature": 24.5,
  "irradiance": 702.1,
  "humidity": 40.2
}
```

#### `dto/MeasurementSeriesDto.java`
시계열 측정 데이터 컬렉션
```java
{
  "plantId": 101,
  "series": [ /* MeasurementDto 배열 */ ]
}
```

#### `dto/DashboardSummaryDto.java`
대시보드 요약 정보
```java
{
  "currentPowerKw": 92.4,
  "todayGenerationKwh": 538.2,
  "efficiencyPercent": 87.1,
  "lastUpdatedAt": "2026-04-05T14:30:00Z",
  "latestAnomaly": {
    "exists": true,
    "eventId": 9001,
    "severity": "HIGH",
    "summary": "예상 대비 발전량 급감"
  }
}
```

### 서비스

#### `service/MeasurementService.java` (신규)
측정 데이터 및 대시보드 조회 로직
```java
// 대시보드 요약 조회
DashboardSummaryDto getDashboardSummary(Long plantId, Long userId)

// 시계열 측정 데이터 조회
MeasurementSeriesDto getMeasurementSeries(Long plantId, Long userId,
                                          LocalDateTime from, LocalDateTime to)
```

### 컨트롤러

#### `controller/DashboardController.java` (재구성)
- 경로 변경: `/api/dashboard` → `/api/v1/plants/{plantId}`
- 엔드포인트:
  - `GET /dashboard/summary` - 대시보드 요약
  - `GET /measurements` - 시계열 측정 데이터

#### `controller/AnomalyController.java` (재구성)
- 경로 변경: `/api/anomalies` → `/api/v1/plants/{plantId}/anomalies`
- 엔드포인트:
  - `GET /` - 이상 탐지 목록

### 엔티티 변경

#### `entity/EnergyLog.java`
필드 추가:
- `powerKw` - 발전 전력 (kW) (기존 `actualGeneration` 대체)
- `temperature` - 온도 (℃)
- `irradiance` - 일사량 (W/m²)
- `humidity` - 습도 (%)

#### `entity/Anomaly.java`
필드 변경:
- `severity`: `Double` → `String` (LOW, MEDIUM, HIGH)
- `description` → `summary` (요약)
- 필드 추가:
  - `cause` - 원인
  - `recommendedAction` - 권장 조치
  - `status` - DETECTED, ACKNOWLEDGED, RESOLVED

### DTO 변경

#### `dto/AnomalyDto.java` (전면 재작성)
명세 기준으로 필드 개선
```java
{
  "eventId": 9001,
  "type": "POWER",
  "severity": "HIGH",
  "detectedAt": "2026-04-05T13:40:00Z",
  "summary": "예상 대비 발전량 28% 감소",
  "status": "OPEN",
  "cause": "일사량 대비 출력 저하",
  "recommendedAction": "패널 상태 점검",
  "xaiExplanation": "..."
}
```

### 저장소

#### `repository/EnergyLogRepository.java` (확장)
```java
Optional<EnergyLog> findTopByPowerPlantIdOrderByTimestampDesc(Long powerPlantId)
```

## 현재 구현된 API

### 대시보드 요약
```
GET /api/v1/plants/{plantId}/dashboard/summary
Authorization: Bearer <token>

Response 200:
{
  "success": true,
  "data": {
    "currentPowerKw": 92.4,
    "todayGenerationKwh": 538.2,
    "efficiencyPercent": 87.1,
    "lastUpdatedAt": "2026-04-05T14:30:00Z",
    "latestAnomaly": {
      "exists": true,
      "eventId": 9001,
      "severity": "HIGH",
      "summary": "예상 대비 발전량 급감"
    }
  },
  "message": "대시보드 요약 조회 성공"
}
```

### 시계열 측정 데이터
```
GET /api/v1/plants/{plantId}/measurements?from=2026-04-05T00:00:00Z&to=2026-04-06T00:00:00Z
Authorization: Bearer <token>

Response 200:
{
  "success": true,
  "data": {
    "plantId": 101,
    "series": [
      {
        "measuredAt": "2026-04-05T10:00:00Z",
        "powerKw": 81.2,
        "temperature": 24.5,
        "irradiance": 702.1,
        "humidity": 40.2
      },
      {
        "measuredAt": "2026-04-05T10:05:00Z",
        "powerKw": 82.7,
        "temperature": 24.7,
        "irradiance": 710.8,
        "humidity": 39.8
      }
    ]
  },
  "message": "계측 데이터 조회 성공"
}
```

### 이상 탐지 목록
```
GET /api/v1/plants/{plantId}/anomalies?limit=10
Authorization: Bearer <token>

Response 200:
{
  "success": true,
  "data": [
    {
      "eventId": 9001,
      "type": "POWER",
      "severity": "HIGH",
      "detectedAt": "2026-04-05T13:40:00Z",
      "summary": "예상 대비 발전량 28% 감소",
      "status": "OPEN",
      "cause": "일사량 대비 출력 저하",
      "recommendedAction": "패널 상태 점검",
      "xaiExplanation": null
    }
  ],
  "message": "이상 이벤트 목록 조회 성공"
}
```

## 테스트 결과
✅ 빌드 성공
✅ 테스트 통과

## 주요 변경 사항

| 항목 | 이전 | 이후 |
|------|------|------|
| 경로 | `/api/dashboard/power-plant/{id}` | `/api/v1/plants/{id}/dashboard/summary` |
| 경로 | `/api/anomalies/power-plant/{id}` | `/api/v1/plants/{id}/anomalies` |
| Severity | Double (0~1) | String (LOW, MEDIUM, HIGH) |
| 필드명 | `description` | `summary`, `cause`, `recommendedAction` |
| 응답 형식 | 개별 DTO | `ApiResponse` 래퍼 |

## 엔티티 관계도

```
PowerPlant
  ├── User
  ├── EnergyLog (1:N)
  │   ├── powerKw
  │   ├── temperature
  │   ├── irradiance
  │   └── humidity
  └── Anomaly (1:N)
      ├── summary
      ├── cause
      ├── recommendedAction
      └── severity (HIGH, MEDIUM, LOW)
```

## 다음 단계

### P1 - 예측 API
- `Forecast`, `ForecastExplanation` 엔티티 추가
- `GET /plants/{plantId}/forecasts` 구현
- `GET /plants/{plantId}/forecasts/explanations` 구현

### P2 - 이상 탐지 상세 및 상태 변경
- `GET /plants/{plantId}/anomalies/{eventId}` 구현
- `PATCH /plants/{plantId}/anomalies/{eventId}/status` 구현

### P3 - AI 연동
- AI 클라이언트 HTTP 요청 구현
- 비동기 처리 및 재시도 정책
- 이미지 분석 API

### P4 - 챗 및 알림
- 챗 세션/메시지 API
- 알림 설정 및 메일 발송

## 파일 요약

생성:
- `dto/MeasurementDto.java`
- `dto/MeasurementSeriesDto.java`
- `dto/DashboardSummaryDto.java`
- `service/MeasurementService.java`

수정:
- `entity/EnergyLog.java`
- `entity/Anomaly.java`
- `dto/AnomalyDto.java`
- `repository/EnergyLogRepository.java`
- `controller/DashboardController.java`
- `controller/AnomalyController.java`
- `service/AnomalyService.java`

## 빌드 정보
- 빌드 시간: ~51s
- 테스트 시간: ~32s
- 상태: ✅ 모두 성공



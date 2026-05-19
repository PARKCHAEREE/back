# SolarWise AI API 명세서
백엔드 - AI 내부 연동 API

## 1. 발전량 예측 요청

### Method
- `POST`

### Path
- `/internal/forecast/predict`

### Request

```json
{
  "plantId": 101,
  "requestedAt": "2026-04-05T14:00:00Z",
  "history": [
    {
      "measuredAt": "2026-04-05T10:00:00Z",
      "powerKw": 81.2,
      "temperature": 24.5,
      "irradiance": 702.1,
      "humidity": 40.2
    }
  ],
  "weatherForecast": [
    {
      "targetTime": "2026-04-06T10:00:00Z",
      "temperature": 23.1,
      "irradiance": 690.4,
      "humidity": 43.0,
      "cloudCover": 0.56
    }
  ]
}
```

### Response 200

```json
{
  "forecastSeries": [
    {
      "targetTime": "2026-04-06T10:00:00Z",
      "predictedPowerKw": 79.1,
      "confidence": 0.89
    }
  ],
  "explanations": [
    {
      "targetTime": "2026-04-06T10:00:00Z",
      "summary": "구름량 증가가 예측 하락의 가장 큰 원인입니다."
    }
  ]
}
```

## 2. 발전량 이상 감지 요청

### Method
- `POST`

### Path
- `/internal/anomaly/power-detect`

### Request

```json
{
  "plantId": 101,
  "measurements": [
    {
      "measuredAt": "2026-04-05T13:40:00Z",
      "powerKw": 55.1,
      "temperature": 25.0,
      "irradiance": 710.0
    }
  ],
  "forecastReference": [
    {
      "targetTime": "2026-04-05T13:40:00Z",
      "predictedPowerKw": 76.5
    }
  ]
}
```

### Response 200

```json
{
  "anomalyDetected": true,
  "severity": "HIGH",
  "summary": "예상 대비 발전량 28% 감소",
  "cause": "정상 일사량 대비 출력 급감",
  "recommendedAction": "패널 상태 및 인버터 연결 여부를 점검하세요."
}
```

## 3. 패널 이미지 분석 요청

### Method
- `POST`

### Path
- `/internal/anomaly/vision-detect`

### Request

```json
{
  "plantId": 101,
  "imageUrl": "https://storage.solarwise.com/panels/101/20260405_134500.jpg",
  "capturedAt": "2026-04-05T13:45:00Z"
}
```

### Response 200

```json
{
  "detectedIssue": "CRACK",
  "severity": "HIGH",
  "confidence": 0.95,
  "summary": "패널 표면 크랙이 감지되었습니다.",
  "recommendedAction": "즉시 현장 점검과 교체 검토가 필요합니다."
}
```

## 4. XAI 설명 생성 요청

### Method
- `POST`

### Path
- `/internal/xai/explain`

### Request

```json
{
  "plantId": 101,
  "eventId": 9001,
  "context": {
    "anomalyType": "POWER",
    "forecast": {
      "predictedPowerKw": 76.5
    },
    "actual": {
      "powerKw": 55.1
    },
    "weather": {
      "irradiance": 710.0,
      "cloudCover": 0.12
    }
  }
}
```

### Response 200

```json
{
  "summary": "기상 조건은 양호했으나 실제 출력이 크게 낮아 설비 상태 이상 가능성이 높습니다.",
  "topFactors": [
    {
      "name": "actual_vs_predicted_gap",
      "impact": 0.61
    },
    {
      "name": "panel_surface_condition",
      "impact": 0.22
    }
  ]
}
```
# SolarWise 프론트엔드 API 명세서

본 문서는 프론트엔드가 백엔드로 요청할 API만 정의합니다.
백엔드와 AI 서버 간 내부 연동 API는 본 문서에 포함하지 않습니다.

- 기준 버전: `v1`
- Base URL: `https://api.solarwise.com/api/v1`
- Content-Type: `application/json; charset=utf-8`
- 시간 포맷: ISO-8601 (`2026-04-05T14:30:00Z`)
- 인증 방식: `Authorization: Bearer <accessToken>`

## 1. 포함 범위

다음 화면/기능에서 프론트엔드가 직접 호출하는 API만 포함합니다.

- 로그인 / 회원가입
- 발전소 목록 / 선택
- 실시간 발전량 대시보드
- 발전량 예측 그래프
- 이상 감지 목록 / 상세
- 원인 설명 챗
- 알림 설정 / 메일 발송 이력

## 2. 공통 응답 형식

### 성공

```json
{
  "success": true,
  "data": {},
  "message": "요청이 성공했습니다."
}
```

### 실패

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "요청 값이 올바르지 않습니다.",
    "details": [
      {
        "field": "email",
        "reason": "INVALID_FORMAT"
      }
    ]
  }
}
```

## 3. 인증 API

## 3-1. 회원가입

- Method: `POST`
- Path: `/auth/signup`
- 설명: 사용자 회원가입

### Request

```json
{
  "name": "홍길동",
  "email": "user@solarwise.com",
  "password": "Password123!",
  "role": "OWNER"
}
```

### Response 201

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "name": "홍길동",
    "email": "user@solarwise.com",
    "role": "OWNER"
  },
  "message": "회원가입이 완료되었습니다."
}
```

## 3-2. 로그인

- Method: `POST`
- Path: `/auth/login`
- 설명: 이메일/비밀번호 로그인

### Request

```json
{
  "email": "user@solarwise.com",
  "password": "Password123!"
}
```

### Response 200

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "user": {
      "userId": 1,
      "name": "홍길동",
      "email": "user@solarwise.com",
      "role": "OWNER"
    }
  },
  "message": "로그인에 성공했습니다."
}
```

## 3-3. 로그아웃

- Method: `POST`
- Path: `/auth/logout`
- 설명: 현재 로그인 세션 종료

### Response 200

```json
{
  "success": true,
  "data": null,
  "message": "로그아웃되었습니다."
}
```

## 3-4. 내 정보 조회

- Method: `GET`
- Path: `/users/me`
- 설명: 로그인한 사용자 정보 조회

### Response 200

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "name": "홍길동",
    "email": "user@solarwise.com",
    "role": "OWNER"
  },
  "message": "사용자 정보 조회 성공"
}
```

## 4. 발전소 API

## 4-1. 발전소 목록 조회

- Method: `GET`
- Path: `/plants`
- 설명: 로그인한 사용자의 발전소 목록 조회

### Response 200

```json
{
  "success": true,
  "data": [
    {
      "plantId": 101,
      "name": "전북 익산 1호 발전소",
      "location": "전북 익산시",
      "capacityKw": 120.5,
      "status": "ACTIVE"
    }
  ],
  "message": "발전소 목록 조회 성공"
}
```

## 4-2. 발전소 상세 조회

- Method: `GET`
- Path: `/plants/{plantId}`
- 설명: 선택한 발전소 상세 정보 조회

### Response 200

```json
{
  "success": true,
  "data": {
    "plantId": 101,
    "name": "전북 익산 1호 발전소",
    "location": "전북 익산시",
    "capacityKw": 120.5,
    "status": "ACTIVE",
    "inverterModel": "INV-3000",
    "sensorSerialNumber": "SNSR-2026-0001"
  },
  "message": "발전소 상세 조회 성공"
}
```

## 4-3. 발전소 등록

- Method: `POST`
- Path: `/plants`
- 설명: 로그인한 사용자의 발전소 신규 등록

### Request

```json
{
  "name": "전북 익산 1호 발전소",
  "location": "전북특별자치도 익산시",
  "capacityKw": 120.5,
  "panelCount": 320,
  "inverterModel": "INV-3000",
  "sensorSerialNumber": "SNSR-2026-0001",
  "latitude": 35.95,
  "longitude": 126.95
}
```

> 참고: `capacity` 키도 별칭으로 허용됩니다.

### Response 201

```json
{
  "success": true,
  "data": {
    "plantId": 102,
    "name": "전북 익산 1호 발전소",
    "location": "전북특별자치도 익산시",
    "capacityKw": 120.5,
    "status": "ACTIVE",
    "inverterModel": "INV-3000",
    "sensorSerialNumber": "SNSR-2026-0001"
  },
  "message": "발전소가 등록되었습니다."
}
```

## 4-4. 발전소 수정

- Method: `PUT`
- Path: `/plants/{plantId}`
- 설명: 로그인한 사용자의 발전소 기본 정보 수정

### Request

```json
{
  "name": "전북 익산 1호 발전소(수정)",
  "location": "전북특별자치도 익산시",
  "capacityKw": 130.0,
  "panelCount": 340,
  "inverterModel": "INV-3500",
  "sensorSerialNumber": "SNSR-2026-0001",
  "latitude": 35.96,
  "longitude": 126.96
}
```

### Response 200

```json
{
  "success": true,
  "data": {
    "plantId": 102,
    "name": "전북 익산 1호 발전소(수정)",
    "location": "전북특별자치도 익산시",
    "capacityKw": 130.0,
    "status": "ACTIVE",
    "inverterModel": "INV-3500",
    "sensorSerialNumber": "SNSR-2026-0001"
  },
  "message": "발전소 정보가 수정되었습니다."
}
```

## 4-5. 발전소 삭제

- Method: `DELETE`
- Path: `/plants/{plantId}`
- 설명: 로그인한 사용자의 발전소 비활성화(소프트 삭제)

### Response 200

```json
{
  "success": true,
  "data": null,
  "message": "발전소가 삭제되었습니다."
}
```

## 5. 대시보드 API

## 5-1. 대시보드 요약 조회

- Method: `GET`
- Path: `/plants/{plantId}/dashboard/summary`
- 설명: 현재 발전 상태, 금일 발전량, 최근 이상 여부 조회

### Response 200

```json
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

## 5-2. 실측 발전량 시계열 조회

- Method: `GET`
- Path: `/plants/{plantId}/measurements`
- 설명: 실시간/기간별 발전량 그래프용 데이터 조회

### Query

- `from`: 조회 시작 시각
- `to`: 조회 종료 시각
- `interval`: `5m`, `1h`, `1d`

### Response 200

```json
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

## 6. 발전량 예측 API

## 6-1. 예측 발전량 조회

- Method: `GET`
- Path: `/plants/{plantId}/forecasts`
- 설명: 향후 2~3일 예측 발전량 조회

### Query

- `from`: 예측 시작 시각
- `to`: 예측 종료 시각
- `interval`: `1h`, `1d`

### Response 200

```json
{
  "success": true,
  "data": {
    "plantId": 101,
    "generatedAt": "2026-04-05T14:00:00Z",
    "series": [
      {
        "targetTime": "2026-04-06T09:00:00Z",
        "predictedPowerKw": 74.6,
        "confidence": 0.91
      },
      {
        "targetTime": "2026-04-06T10:00:00Z",
        "predictedPowerKw": 79.1,
        "confidence": 0.89
      }
    ]
  },
  "message": "예측 데이터 조회 성공"
}
```

## 6-2. 예측 설명 조회

- Method: `GET`
- Path: `/plants/{plantId}/forecasts/explanations`
- 설명: 특정 예측 시점에 대한 원인 설명 조회

### Query

- `targetTime`: 설명이 필요한 예측 시각

### Response 200

```json
{
  "success": true,
  "data": {
    "targetTime": "2026-04-06T10:00:00Z",
    "summary": "운량 증가와 일사량 감소가 예측 발전량 하락에 가장 큰 영향을 주었습니다.",
    "factors": [
      {
        "name": "cloudCover",
        "impact": -0.42,
        "description": "구름량 증가로 일사량이 감소했습니다."
      },
      {
        "name": "irradiance",
        "impact": -0.31,
        "description": "시간대 평균 대비 낮은 일사량이 예측되었습니다."
      }
    ]
  },
  "message": "예측 설명 조회 성공"
}
```

## 7. 이상 감지 API

## 7-1. 이상 이벤트 목록 조회

- Method: `GET`
- Path: `/plants/{plantId}/anomalies`
- 설명: 발전량 이상 및 패널 이상 이벤트 목록 조회

### Query

- `status`: `OPEN`, `ACKNOWLEDGED`, `RESOLVED`
- `type`: `POWER`, `VISION`
- `severity`: `LOW`, `MEDIUM`, `HIGH`

### Response 200

```json
{
  "success": true,
  "data": [
    {
      "eventId": 9001,
      "type": "POWER",
      "severity": "HIGH",
      "detectedAt": "2026-04-05T13:40:00Z",
      "summary": "예상 대비 발전량 28% 감소",
      "status": "OPEN"
    },
    {
      "eventId": 9002,
      "type": "VISION",
      "severity": "MEDIUM",
      "detectedAt": "2026-04-05T13:50:00Z",
      "summary": "패널 표면 오염 의심",
      "status": "OPEN"
    }
  ],
  "message": "이상 이벤트 목록 조회 성공"
}
```

## 7-2. 이상 이벤트 상세 조회

- Method: `GET`
- Path: `/plants/{plantId}/anomalies/{eventId}`
- 설명: 이상 원인, 설명, 권장 조치 조회

### Response 200

```json
{
  "success": true,
  "data": {
    "eventId": 9001,
    "type": "POWER",
    "severity": "HIGH",
    "detectedAt": "2026-04-05T13:40:00Z",
    "summary": "예상 대비 발전량 28% 감소",
    "cause": "일사량 대비 실제 출력이 낮아 패널 오염 또는 음영 가능성이 높습니다.",
    "recommendedAction": "패널 표면 오염 여부와 주변 음영 발생 요소를 우선 점검하세요.",
    "xaiExplanation": {
      "summary": "일사량은 정상 범위였지만 출력만 급감해 설비 이상 가능성이 높습니다.",
      "topFactors": [
        "power_drop_rate",
        "irradiance_gap",
        "panel_surface_state"
      ]
    }
  },
  "message": "이상 이벤트 상세 조회 성공"
}
```

## 7-3. 이상 이벤트 상태 변경

- Method: `PATCH`
- Path: `/plants/{plantId}/anomalies/{eventId}/status`
- 설명: 프론트엔드에서 확인 완료/해결 처리

### Request

```json
{
  "status": "ACKNOWLEDGED"
}
```

### Response 200

```json
{
  "success": true,
  "data": {
    "eventId": 9001,
    "status": "ACKNOWLEDGED"
  },
  "message": "이상 이벤트 상태가 변경되었습니다."
}
```

## 8. 원인 설명 챗 API

## 8-1. 챗 세션 생성

- Method: `POST`
- Path: `/plants/{plantId}/chat/sessions`
- 설명: 이상 이벤트 기반 챗 세션 시작

### Request

```json
{
  "eventId": 9001
}
```

### Response 201

```json
{
  "success": true,
  "data": {
    "sessionId": "chat_20260405_001",
    "welcomeMessage": "이번 이상 이벤트의 원인과 조치 방법을 설명드릴게요."
  },
  "message": "챗 세션이 생성되었습니다."
}
```

## 8-2. 챗 메시지 전송

- Method: `POST`
- Path: `/plants/{plantId}/chat/sessions/{sessionId}/messages`
- 설명: 원인 설명 챗 질문 전송

### Request

```json
{
  "message": "왜 발전량이 갑자기 떨어졌나요?"
}
```

### Response 200

```json
{
  "success": true,
  "data": {
    "answer": "현재 이벤트는 일사량 대비 출력 저하 패턴이 커서 패널 오염 또는 부분 음영 가능성이 높습니다.",
    "references": [
      "forecast_explanation",
      "latest_measurements",
      "vision_analysis"
    ]
  },
  "message": "답변 생성 성공"
}
```

## 9. 알림 API

## 9-1. 알림 설정 조회

- Method: `GET`
- Path: `/plants/{plantId}/alert-settings`
- 설명: 메일 알림 설정 조회

### Response 200

```json
{
  "success": true,
  "data": {
    "plantId": 101,
    "emailEnabled": true,
    "emailAddress": "alert@solarwise.com",
    "severityThreshold": "MEDIUM"
  },
  "message": "알림 설정 조회 성공"
}
```

## 9-2. 알림 설정 수정

- Method: `PUT`
- Path: `/plants/{plantId}/alert-settings`
- 설명: 메일 알림 수신 여부 및 기준 저장

### Request

```json
{
  "emailEnabled": true,
  "emailAddress": "alert@solarwise.com",
  "severityThreshold": "MEDIUM"
}
```

### Response 200

```json
{
  "success": true,
  "data": {
    "plantId": 101,
    "emailEnabled": true,
    "emailAddress": "alert@solarwise.com",
    "severityThreshold": "MEDIUM"
  },
  "message": "알림 설정이 저장되었습니다."
}
```

## 9-3. 알림 이력 조회

- Method: `GET`
- Path: `/plants/{plantId}/alerts`
- 설명: 메일 발송 이력 조회

### Response 200

```json
{
  "success": true,
  "data": [
    {
      "alertId": 3001,
      "eventId": 9001,
      "channel": "EMAIL",
      "sentTo": "alert@solarwise.com",
      "sentAt": "2026-04-05T13:41:00Z",
      "status": "SENT"
    }
  ],
  "message": "알림 이력 조회 성공"
}
```

## 10. 화면별 API 매핑

### 로그인 / 회원가입 페이지

- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/logout`
- `GET /users/me`

### 발전소 선택 / 대시보드 페이지

- `GET /plants`
- `POST /plants`
- `GET /plants/{plantId}`
- `PUT /plants/{plantId}`
- `DELETE /plants/{plantId}`
- `GET /plants/{plantId}/dashboard/summary`
- `GET /plants/{plantId}/measurements`
- `GET /plants/{plantId}/forecasts`
- `GET /plants/{plantId}/forecasts/explanations`

### 이상 감지 상세 / 원인 설명 페이지

- `GET /plants/{plantId}/anomalies`
- `GET /plants/{plantId}/anomalies/{eventId}`
- `PATCH /plants/{plantId}/anomalies/{eventId}/status`
- `POST /plants/{plantId}/chat/sessions`
- `POST /plants/{plantId}/chat/sessions/{sessionId}/messages`

### 알림 설정 페이지

- `GET /plants/{plantId}/alert-settings`
- `PUT /plants/{plantId}/alert-settings`
- `GET /plants/{plantId}/alerts`

## 11. 프론트엔드 구현 우선순위

1. `POST /auth/signup`
2. `POST /auth/login`
3. `GET /users/me`
4. `GET /plants`
5. `POST /plants`
6. `PUT /plants/{plantId}`
7. `DELETE /plants/{plantId}`
8. `GET /plants/{plantId}/dashboard/summary`
9. `GET /plants/{plantId}/measurements`
10. `GET /plants/{plantId}/forecasts`
11. `GET /plants/{plantId}/anomalies`
12. `GET /plants/{plantId}/anomalies/{eventId}`
13. `POST /plants/{plantId}/chat/sessions`
14. `POST /plants/{plantId}/chat/sessions/{sessionId}/messages`
15. `GET /plants/{plantId}/alert-settings`
16. `PUT /plants/{plantId}/alert-settings`


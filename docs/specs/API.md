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
- `GET /plants/{plantId}`
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
5. `GET /plants/{plantId}/dashboard/summary`
6. `GET /plants/{plantId}/measurements`
7. `GET /plants/{plantId}/forecasts`
8. `GET /plants/{plantId}/anomalies`
9. `GET /plants/{plantId}/anomalies/{eventId}`
10. `POST /plants/{plantId}/chat/sessions`
11. `POST /plants/{plantId}/chat/sessions/{sessionId}/messages`
12. `GET /plants/{plantId}/alert-settings`
13. `PUT /plants/{plantId}/alert-settings`
# SolarWise 서비스 API 명세서

본 문서는 SolarWise 프로젝트의 핵심 기능 구현에 필요한 요구 기능과 API 명세를 정의합니다.

- 프로젝트 구성: 프론트엔드(React), 백엔드(Spring Boot + MySQL), AI(PyTorch + OpenCV + XAI)
- 핵심 기능:
  - 실시간 발전량 계측 및 예측 그래프 제공
  - 이상 감지 및 원인 설명 챗
  - 로그인 / 회원가입
  - 이상 감지 시 메일 알림
- 기준 버전: `v1`

## 1. 시스템 역할

### 프론트엔드
- 사용자 인증 화면
- 발전소 대시보드
- 발전량 실측/예측 그래프
- 이상 감지 내역 및 상세 원인 확인
- 원인 설명 챗 UI
- 알림 설정 화면

### 백엔드
- 인증/인가
- 사용자, 발전소, 센서, 알림 설정 관리
- 실시간 계측 데이터 저장 및 조회
- 예측 결과, 이상 탐지 결과, 이미지 분석 결과 저장
- 메일 알림 발송
- 프론트엔드용 통합 API 제공
- AI 서버와 연동

### AI 서버
- 발전량 예측 Transformer 모델 추론
- 발전량 이상 감지
- 패널 이미지 비전 분석(OpenCV 포함)
- XAI 기반 원인 설명 생성

## 2. 필수 기능 정의

### 2-1. 회원 기능
- 회원가입
- 로그인 / 로그아웃
- 내 정보 조회

### 2-2. 발전소 관리 기능
- 사용자별 발전소 등록/조회
- 발전소 기본 정보 관리
- 센서/인버터 식별 정보 관리

### 2-3. 발전량 모니터링 기능
- 현재 발전량 조회
- 기간별 발전량 시계열 조회
- 온도, 일사량 등 보조 지표 조회
- 실측값 그래프 렌더링

### 2-4. 발전량 예측 기능
- 향후 2~3일 예측 발전량 조회
- 예측 구간별 신뢰도 조회
- 예측 근거(XAI) 조회

### 2-5. 이상 감지 기능
- 발전량 패턴 이상 감지
- 패널 이미지 기반 결함/오염 감지
- 이상 상세 사유 및 권장 조치 제공

### 2-6. 챗 기능
- 이상 이벤트를 기준으로 질의응답
- XAI 기반 설명 요약 제공
- 발전소 상태에 대한 후속 질문 지원

### 2-7. 알림 기능
- 이상 감지 시 메일 발송
- 알림 수신 여부 설정
- 알림 이력 조회

## 3. 공통 규칙

- Front Base URL: `https://api.solarwise.com/api/v1`
- AI Internal Base URL: `https://ai.solarwise.internal/api/v1`
- Content-Type: `application/json; charset=utf-8`
- 시간 포맷: ISO-8601 (`2026-04-05T14:30:00Z`)
- 인증 방식: `Authorization: Bearer <access_token>`

### 공통 응답 형식

```json
{
  "success": true,
  "data": {},
  "message": "요청이 성공했습니다."
}
```

### 공통 에러 형식

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

## 4. 주요 데이터 모델

### User
- `userId`
- `name`
- `email`
- `password`
- `role`
- `createdAt`

### Plant
- `plantId`
- `userId`
- `name`
- `location`
- `capacityKw`
- `status`
- `createdAt`

### PowerMeasurement
- `measurementId`
- `plantId`
- `measuredAt`
- `powerKw`
- `temperature`
- `irradiance`
- `humidity`

### Forecast
- `forecastId`
- `plantId`
- `forecastAt`
- `targetTime`
- `predictedPowerKw`
- `confidence`

### AnomalyEvent
- `eventId`
- `plantId`
- `type`
- `severity`
- `detectedAt`
- `summary`
- `cause`
- `recommendedAction`

### AlertSetting
- `alertSettingId`
- `userId`
- `plantId`
- `emailEnabled`
- `emailAddress`
- `severityThreshold`

## 5. 프론트엔드 공개 API

## 5-1. 인증 API

### 회원가입
- Method: `POST`
- Path: `/auth/signup`
- 설명: 일반 사용자 회원가입

#### Request

```json
{
  "name": "홍길동",
  "email": "user@solarwise.com",
  "password": "Password123!",
  "role": "OWNER"
}
```

#### Response 201

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "name": "홍길동",
    "email": "user@solarwise.com"
  },
  "message": "회원가입이 완료되었습니다."
}
```

### 로그인
- Method: `POST`
- Path: `/auth/login`
- 설명: 이메일/비밀번호 로그인

#### Request

```json
{
  "email": "user@solarwise.com",
  "password": "Password123!"
}
```

#### Response 200

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

### 로그아웃
- Method: `POST`
- Path: `/auth/logout`
- 설명: 현재 토큰 세션 종료

### 내 정보 조회
- Method: `GET`
- Path: `/users/me`
- 설명: 로그인한 사용자 정보 조회

## 5-2. 발전소 관리 API

### 발전소 목록 조회
- Method: `GET`
- Path: `/plants`
- 설명: 로그인한 사용자의 발전소 목록 조회

#### Response 200

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

### 발전소 등록
- Method: `POST`
- Path: `/plants`
- 설명: 새로운 발전소 등록

#### Request

```json
{
  "name": "전북 익산 1호 발전소",
  "location": "전북 익산시",
  "capacityKw": 120.5,
  "inverterModel": "INV-3000",
  "sensorSerialNumber": "SNSR-2026-0001"
}
```

### 발전소 상세 조회
- Method: `GET`
- Path: `/plants/{plantId}`
- 설명: 발전소 상세 정보 조회

## 5-3. 대시보드 / 실시간 계측 API

### 대시보드 요약 조회
- Method: `GET`
- Path: `/plants/{plantId}/dashboard/summary`
- 설명: 현재 발전 상태, 금일 발전량, 최근 이상 여부 조회

#### Response 200

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

### 시계열 발전량 조회
- Method: `GET`
- Path: `/plants/{plantId}/measurements`
- 설명: 실측 발전량 그래프용 시계열 데이터 조회

#### Query
- `from`: 조회 시작 시간
- `to`: 조회 종료 시간
- `interval`: `5m`, `1h`, `1d`

#### Response 200

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

## 5-4. 발전량 예측 API

### 예측 발전량 조회
- Method: `GET`
- Path: `/plants/{plantId}/forecasts`
- 설명: 향후 2~3일 예측 발전량 조회

#### Query
- `from`: 예측 시작 시간
- `to`: 예측 종료 시간
- `interval`: `1h`, `1d`

#### Response 200

```json
{
  "success": true,
  "data": {
    "plantId": 101,
    "model": "transformer-v1",
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

### 예측 설명(XAI) 조회
- Method: `GET`
- Path: `/plants/{plantId}/forecasts/explanations`
- 설명: 예측 결과에 영향을 준 주요 요인 설명 조회

#### Query
- `targetTime`: 설명이 필요한 예측 시점

#### Response 200

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

## 5-5. 이상 감지 API

### 이상 이벤트 목록 조회
- Method: `GET`
- Path: `/plants/{plantId}/anomalies`
- 설명: 발전소의 이상 감지 이벤트 목록 조회

#### Query
- `status`: `OPEN`, `ACKNOWLEDGED`, `RESOLVED`
- `type`: `POWER`, `VISION`
- `severity`: `LOW`, `MEDIUM`, `HIGH`

#### Response 200

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

### 이상 이벤트 상세 조회
- Method: `GET`
- Path: `/plants/{plantId}/anomalies/{eventId}`
- 설명: 이상 이벤트 원인, 근거, 권장 조치 조회

#### Response 200

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

### 이상 이벤트 확인 처리
- Method: `PATCH`
- Path: `/plants/{plantId}/anomalies/{eventId}/status`
- 설명: 이벤트 상태 변경

#### Request

```json
{
  "status": "ACKNOWLEDGED"
}
```

## 5-6. 이미지 분석 API

### 패널 이미지 업로드 및 분석 요청
- Method: `POST`
- Path: `/plants/{plantId}/vision-analyses`
- 설명: 패널 이미지를 업로드하고 비전 AI 분석 요청

#### Request
- `multipart/form-data`
- 필드:
  - `image`: 이미지 파일
  - `capturedAt`: 촬영 시각
  - `cameraLocation`: 촬영 위치

#### Response 202

```json
{
  "success": true,
  "data": {
    "analysisId": 7001,
    "status": "PROCESSING"
  },
  "message": "이미지 분석이 요청되었습니다."
}
```

### 이미지 분석 결과 조회
- Method: `GET`
- Path: `/plants/{plantId}/vision-analyses/{analysisId}`
- 설명: 패널 이미지 분석 결과 조회

#### Response 200

```json
{
  "success": true,
  "data": {
    "analysisId": 7001,
    "status": "COMPLETED",
    "result": {
      "detectedIssue": "DUST",
      "severity": "MEDIUM",
      "confidence": 0.93,
      "summary": "패널 표면 먼지 축적으로 발전 효율 저하가 예상됩니다.",
      "recommendedAction": "청소 주기를 앞당기고 청소 후 재촬영을 권장합니다."
    }
  },
  "message": "이미지 분석 결과 조회 성공"
}
```

## 5-7. 원인 설명 챗 API

### 챗 세션 생성
- Method: `POST`
- Path: `/plants/{plantId}/chat/sessions`
- 설명: 이상 이벤트 기반 챗 세션 생성

#### Request

```json
{
  "eventId": 9001
}
```

#### Response 201

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

### 챗 질의
- Method: `POST`
- Path: `/plants/{plantId}/chat/sessions/{sessionId}/messages`
- 설명: 이상 원인 설명 챗 메시지 전송

#### Request

```json
{
  "message": "왜 발전량이 갑자기 떨어졌나요?"
}
```

#### Response 200

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

## 5-8. 알림 API

### 알림 설정 조회
- Method: `GET`
- Path: `/plants/{plantId}/alert-settings`
- 설명: 메일 알림 설정 조회

### 알림 설정 수정
- Method: `PUT`
- Path: `/plants/{plantId}/alert-settings`
- 설명: 메일 알림 수신 여부 및 기준 설정

#### Request

```json
{
  "emailEnabled": true,
  "emailAddress": "alert@solarwise.com",
  "severityThreshold": "MEDIUM"
}
```

#### Response 200

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

### 알림 이력 조회
- Method: `GET`
- Path: `/plants/{plantId}/alerts`
- 설명: 메일 발송 이력 조회

#### Response 200

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

## 6. 백엔드 - AI 내부 연동 API

백엔드와 AI 서버가 분리되어 있으므로, 아래 내부 API를 기준으로 연동하는 것을 권장합니다.

## 6-1. 발전량 예측 요청

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

## 6-2. 발전량 이상 감지 요청

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

## 6-3. 패널 이미지 분석 요청

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

## 6-4. XAI 설명 생성 요청

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

## 7. 메일 알림 발송 정책

- 이상 이벤트 생성 후 `severity >= 설정 임계치`이면 메일 발송
- 발송 대상:
  - 발전소 소유자
  - 알림 수신 설정 사용자
- 메일 본문 포함 항목:
  - 발전소명
  - 감지 시각
  - 이상 유형
  - 심각도
  - 원인 요약
  - 권장 조치
  - 상세 페이지 링크

### 메일 발송 트리거 예시
1. 계측 데이터 수신
2. 백엔드가 AI 이상 감지 요청
3. AI가 이상 탐지 결과 반환
4. 백엔드가 `anomaly_event` 저장
5. 알림 설정 확인
6. 조건 충족 시 메일 발송
7. `alert_history` 저장

## 8. 권장 DB 테이블

- `users`
- `plants`
- `plant_devices`
- `power_measurements`
- `power_forecasts`
- `forecast_explanations`
- `anomaly_events`
- `vision_analyses`
- `chat_sessions`
- `chat_messages`
- `alert_settings`
- `alert_histories`

## 9. 화면별 API 매핑

### 로그인 / 회원가입 페이지
- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/logout`
- `GET /users/me`

### 발전량 대시보드 페이지
- `GET /plants`
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

### 이미지 업로드 / 패널 점검 페이지
- `POST /plants/{plantId}/vision-analyses`
- `GET /plants/{plantId}/vision-analyses/{analysisId}`

### 알림 설정 페이지
- `GET /plants/{plantId}/alert-settings`
- `PUT /plants/{plantId}/alert-settings`
- `GET /plants/{plantId}/alerts`

## 10. 우선 구현 순서 제안

1. 인증 API
2. 발전소/대시보드 실측 데이터 API
3. 예측 발전량 API
4. 이상 감지 및 상세 조회 API
5. 메일 알림 API
6. 이미지 분석 API
7. 원인 설명 챗 API

## 11. 비고

- 예측 및 이미지 분석은 응답 시간이 길 수 있으므로 비동기 처리(`202 Accepted`)를 일부 도입하는 것이 좋습니다.
- AI 설명 결과는 백엔드가 저장해 프론트엔드가 빠르게 다시 조회할 수 있도록 구성하는 것을 권장합니다.
- 기상청 API는 직접 프론트엔드에서 호출하지 않고, 백엔드가 수집/가공 후 AI 모델 입력으로 전달하는 구조를 권장합니다.


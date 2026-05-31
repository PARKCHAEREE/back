# 대시보드 타임라인 API 계약서 (시연 기준, 1초 고정) - 2026-05-31

## 1) 목적
- 프론트엔드가 대시보드 그래프를 안정적으로 연동할 수 있도록 요청/응답 형식을 고정한다.
- 시연 기준은 `1초 = 가상시간 1시간 전진`이며, speed 변경 기능은 사용하지 않는다.
- 이상 시연은 자동 감지 상시 실행이 아니라 `trigger-power-anomaly` 호출로 미래 데이터 구간을 조작하는 방식이다.

## 2) 공통 규칙
- 인증: `Authorization: Bearer <token>`
- 시간 기준: 서버의 `virtualCurrentTime`
- 응답 래퍼: `ApiResponse` 사용
- 인가: 시뮬레이션 **조작 API**(`tick`, `playback start/stop`, `trigger-*`)는 `ADMIN` 역할 필요

## 3) 타임라인 조회
### 엔드포인트
- `GET /api/v1/plants/{plantId}/dashboard/timeline`

### Query Parameters
- `range`: `DAY | WEEK | MONTH` (기본값 `DAY`)
- `futureHours`: 예측 미래 시간(선택)
- `to`: ISO datetime (선택, 기본값 `virtualNow`)

### 예시 요청
```http
GET /api/v1/plants/1/dashboard/timeline?range=DAY
Authorization: Bearer <token>
```

### 예시 응답
```json
{
  "success": true,
  "data": {
    "plantId": 1,
    "range": "DAY",
    "virtualNow": "2026-03-20T12:00:00Z",
    "windowStart": "2026-03-19T12:00:00Z",
    "windowEnd": "2026-03-20T12:00:00Z",
    "forecastEnd": "2026-03-21T12:00:00Z",
    "actualSeries": [
      { "ts": "2026-03-20T10:00:00Z", "value": 93.5 },
      { "ts": "2026-03-20T11:00:00Z", "value": 95.0 },
      { "ts": "2026-03-20T12:00:00Z", "value": 97.2 }
    ],
    "predictionSeries": [
      { "ts": "2026-03-20T10:00:00Z", "value": 96.1 },
      { "ts": "2026-03-20T11:00:00Z", "value": 97.0 },
      { "ts": "2026-03-20T12:00:00Z", "value": 98.3 },
      { "ts": "2026-03-20T13:00:00Z", "value": 99.1 }
    ],
    "gapSeries": [
      { "ts": "2026-03-20T10:00:00Z", "absGap": 2.6, "gapRate": 0.027 },
      { "ts": "2026-03-20T11:00:00Z", "absGap": 2.0, "gapRate": 0.021 }
    ],
    "anomalyMarkers": [
      {
        "eventId": 145,
        "ts": "2026-03-20T11:00:00Z",
        "type": "POWER",
        "severity": "HIGH",
        "status": "OPEN",
        "summary": "예측 대비 발전량 괴리 초과"
      }
    ]
  },
  "message": "대시보드 타임라인 조회 성공"
}
```

## 4) 재생 제어
### 시작
- `POST /api/v1/simulation/playback/start`

### 정지
- `POST /api/v1/simulation/playback/stop`

### 상태
- `GET /api/v1/simulation/playback/status`

### 상태 응답 예시
```json
{
  "success": true,
  "data": {
    "running": true,
    "tickSeconds": 1,
    "stepHours": 1,
    "virtualCurrentTime": "2026-03-20T12:00:00Z",
    "lastTickAt": "2026-03-20T12:00:00Z"
  },
  "message": "시뮬레이션 자동 재생 상태 조회 성공"
}
```

## 5) 백업용 수동 스텝
- `POST /api/v1/simulation/tick`
- 용도: 자동 재생 장애 시 백업 조작
- 정책: 일반 UI 버튼에는 노출하지 않음 (운영/관리자 메뉴 분리)

## 6) 프론트 연동 체크리스트
- [ ] `playback/start` 호출 후 1초 폴링 시작
- [ ] 1초마다 `dashboard/timeline` 재조회
- [ ] `DAY/WEEK/MONTH` 변경 시 기존 폴링 유지 + query만 변경
- [ ] `anomalyMarkers` 클릭 시 상세 패널 열고 상태 변경 API 연결
- [ ] 화면에는 speed 토글 대신 `1s fixed` 배지 표시



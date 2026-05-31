# 시연용 API 호출 순서 (1초 고정 재생) - 2026-05-31

## 1) 목적
- 시연 중 API 호출 순서를 고정해, 그래프 재생 -> 이상 감지 -> 조치 완료 흐름을 안정적으로 재현한다.
- 기준: `1초 = 가상시간 1시간`, 이상은 트리거 시점 이후 미래 데이터 조작으로 재현.

## 2) 사전 조건
- 관리자 계정(`ADMIN`) JWT 준비
- 시연 대상 `plantId` 확인
- 프론트는 `dashboard/timeline` 1초 폴링 설정

## 3) 기본 헤더
```http
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

## 4) 시연 시퀀스 (권장)
### Step 0. 초기 상태 확인
1. 재생 상태 조회
   - `GET /api/v1/simulation/playback/status`
2. 가상 시간 확인
   - `GET /api/v1/simulation/time`
3. 그래프 데이터 확인
   - `GET /api/v1/plants/{plantId}/dashboard/timeline?range=DAY`

### Step 1. 자동 재생 시작
1. 재생 시작
   - `POST /api/v1/simulation/playback/start`
2. 프론트는 1초마다 타임라인 갱신
   - `GET /api/v1/plants/{plantId}/dashboard/timeline?range=DAY`

### Step 2. 이상 감지 관찰
1. 발전량 이상 트리거 실행 (미래 구간 조작)
   - `POST /api/v1/simulation/trigger-power-anomaly`
   - body 예시:
   ```json
   {
     "plantId": 1,
     "anomalySeverity": "HIGH",
     "differencePercentage": 40.0,
     "durationHours": 2,
     "description": "시연용 미래 구간 발전량 저하"
   }
   ```
2. 재생을 유지하면, 조작된 미래 시점이 `virtualNow` 범위에 들어오면서 그래프 괴리가 관측됨
   - 현재 정책: 트리거 후 약 5초 뒤(=가상시간 5시간 뒤)부터 반영
3. 해당 시점 도달 후 `anomalyMarkers`에 이벤트가 반영됨

### Step 3. 이상 상세 확인 및 상태 변경
1. 이상 목록 확인
   - `GET /api/v1/plants/{plantId}/anomalies`
2. 이상 상세 확인
   - `GET /api/v1/plants/{plantId}/anomalies/{eventId}`
3. 관제 확인 처리
   - `PATCH /api/v1/plants/{plantId}/anomalies/{eventId}/status`
   - body: `{ "status": "ACKNOWLEDGED" }`
4. 조치 완료 처리
   - `PATCH /api/v1/plants/{plantId}/anomalies/{eventId}/status`
   - body: `{ "status": "RESOLVED" }`

### Step 4. 재생 종료
1. 재생 정지
   - `POST /api/v1/simulation/playback/stop`
2. 상태 최종 확인
   - `GET /api/v1/simulation/playback/status`

## 5) 백업 시나리오(장애 대응)
- 자동 재생이 멈추거나 지연될 경우 수동 스텝으로 진행
  - `POST /api/v1/simulation/tick`
- 정책: `tick`은 일반 UI 비노출, 운영/시연 관리자만 사용

## 6) 빠른 데모 스크립트 (10분)
1. `playback/start` 호출 후 20~40초 관찰
2. 타임라인에서 예측 추종 구간 설명
3. `anomalyMarkers` 생성 시점 강조
4. `ACKNOWLEDGED -> RESOLVED` 수동 전환 시연
5. `playback/stop`으로 마무리


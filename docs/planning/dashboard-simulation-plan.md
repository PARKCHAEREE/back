# 대시보드 시계열 재생/이상감지 시뮬레이션 실행 계획 (2026-05-31)

## 0) 시연 의사결정 (2026-05-31 확정)
- 재생 속도는 `1초 = 가상시간 1시간 전진`으로 고정한다.
- `5초` 옵션은 이번 시연 범위에서 제외한다.
- 기존 `POST /api/v1/simulation/tick` API는 삭제하지 않고 **운영/시연 백업용 수동 스텝**으로 유지한다.
  - 프론트 UI에서는 숨기고(또는 관리자 메뉴로 분리), 장애 시 수동 복구/데모 제어용으로만 사용한다.

## 1) 목표
- 대시보드에서 `현재 발전량(과거~현재)`과 `예측 발전량(과거~미래)`을 한 화면에서 시계열로 표시한다.
- 시연을 위해 실제 1시간 간격 데이터를 `1초` 간격으로 재생한다.
- 예측 대비 실측 괴리(gap)가 커지면 이상으로 취급하고, 알림/조치 흐름(`OPEN -> ACKNOWLEDGED -> RESOLVED`)까지 시연한다.

## 2) 데이터/시간 전제
- 데이터 범위: `2026-03-16 00:00` ~ `2026-04-26 23:00` (1시간 간격)
- 시간 기준: 실시간(`now`) 대신 `SimulationService.getVirtualCurrentTime()`를 단일 기준으로 사용
- 재생 속도:
  - `x3600`: 1초마다 가상시간 1시간 전진 (고정)

## 3) 현재 구현 기반 요약 (출발점)
- 가상 시간 제어 API 존재: `/api/v1/simulation/time`, `/api/v1/simulation/tick`
- 수동 이상 트리거 API 존재:
  - `/api/v1/simulation/trigger-power-anomaly`
  - `/api/v1/simulation/trigger-vision-anomaly`
- 대시보드 시계열 조회 API 존재: `/api/v1/plants/{plantId}/measurements`
- 현재 공백:
  - 자동 재생 start/stop/speed 제어 API 부재
  - 실측+예측+괴리+이상마커를 한 번에 주는 전용 타임라인 API 부재
  - day/week/month 윈도우 프리셋 정의 부재

## 4) 목표 아키텍처
### 4.1 핵심 원칙
- DB 원본 시계열(1시간 간격)은 그대로 유지하고, `조회 윈도우`와 `가상 시간`만 움직인다.
- 대시보드 갱신은 "가상 시간 기준 슬라이딩 윈도우"로 구현한다.
- 시연 안정성을 위해 1차는 `폴링`(1초 고정), 2차 확장으로 SSE/WebSocket 검토.

### 4.2 슬라이딩 윈도우 정책
- 공통: `windowEnd = virtualNow`
- `DAY`: `windowStart = virtualNow - 24h`
- `WEEK`: `windowStart = virtualNow - 7d`
- `MONTH`: `windowStart = virtualNow - 30d`
- 예측 곡선은 미래 구간을 위해 `forecastHorizonHours`를 별도 부여
  - 예: DAY=24h, WEEK=48h, MONTH=72h

### 4.3 그래프 데이터 모델(백엔드 응답)
- `actualSeries`: 과거~현재 실측
- `predictionSeries`: 과거~미래 예측
- `gapSeries`: 동일 타임스탬프에서 `abs(actual - prediction)` 및 `gapRate`
- `anomalyMarkers`: 이상 이벤트 시점/심각도/상태
- `meta`: virtualNow, windowRange, playbackSpeed, nextTickAt

## 5) API 설계안
## 5.1 대시보드 타임라인 통합 조회 (신규)
- `GET /api/v1/plants/{plantId}/dashboard/timeline`
- Query
  - `range`: `DAY | WEEK | MONTH`
  - `futureHours`: 기본값(범위별) 또는 사용자 지정
  - `to`: 기본값 `virtualNow`
- Response (요약)
  - `actualSeries[] { ts, value }`
  - `predictionSeries[] { ts, value }`
  - `gapSeries[] { ts, absGap, gapRate }`
  - `anomalyMarkers[] { eventId, ts, severity, status, summary }`
  - `virtualNow`, `range`

## 5.2 재생 제어 (신규)
- `POST /api/v1/simulation/playback/start`
  - body: `{ stepHours: 1 }`
- `POST /api/v1/simulation/playback/stop`
- `GET /api/v1/simulation/playback/status`
  - 반환: 실행 여부, 마지막 tick 시간, virtualNow

## 5.3 기존 API 활용
- 이상 처리: `PATCH /api/v1/plants/{plantId}/anomalies/{eventId}/status`
- 수동 이벤트 주입: `trigger-power-anomaly`, `trigger-vision-anomaly`

## 6) 이상감지(괴리) 규칙 제안
- 지표:
  - `absGap = |actual - prediction|`
  - `gapRate = absGap / max(prediction, epsilon)`
- 기본 임계치 (운영 전 튜닝):
  - `gapRate >= 0.15` 연속 2포인트 -> `MEDIUM`
  - `gapRate >= 0.30` 1포인트 즉시 -> `HIGH`
- 이벤트 라이프사이클:
  - 생성: `OPEN`
  - 관제 확인: `ACKNOWLEDGED`
  - 원인/조치 완료: `RESOLVED`
- 조치 가이드 샘플:
  - 일사량 급감: 기상 원인 안내 + 재관측
  - 설비 이상: 점검 권고 + 현장 확인

## 7) 구현 작업 분해 (체크리스트)
## 7.1 백엔드
- [ ] `DashboardTimelineResponse` DTO 추가 (actual/prediction/gap/markers/meta)
- [ ] `DashboardService`에 range 기반 윈도우 계산 메서드 추가
- [ ] `PlantFeatureLogRepository`에 윈도우+정렬 조회 메서드 점검/추가
- [ ] `AnomalyRepository`에 윈도우 내 이벤트 조회 메서드 추가
- [ ] `DashboardController`에 `/dashboard/timeline` 엔드포인트 추가
- [ ] `SimulationService`에 playback 상태(start/stop/speed) 상태값 추가
- [ ] `SimulationController`에 playback 제어 API 4종 추가
- [ ] `@EnableScheduling` 적용 여부 점검 및 적용

## 7.2 프론트엔드 연동
- [ ] range 스위처(DAY/WEEK/MONTH) + 재생 상태 표시(1초 고정)
- [ ] 2개 라인(실측/예측) + gap 보조선/음영 표시
- [ ] anomaly marker 클릭 시 상세/상태변경 패널 오픈
- [ ] 폴링 주기와 재생 주기 동기화 (1s)

## 7.3 데이터/시나리오
- [ ] 시연용 발전소 1개 기준 데이터 완전성 점검(결측/중복)
- [ ] HIGH 시나리오 1건, MEDIUM 시나리오 1건 사전 주입
- [ ] ACKNOWLEDGED/RESOLVED 데모 스크립트 사전 작성

## 8) 성능/안정성
- 조회 최적화
  - 인덱스: `(power_plant_id, measured_at)` 활용
  - 범위 조회는 `between` 고정 + 필요한 필드만 DTO 매핑
- 응답 크기 제어
  - MONTH 뷰는 downsampling 옵션(예: 3시간 버킷 평균) 제공 고려
- 장애 대응
  - playback 중 예외 발생 시 자동 stop + 상태 API에 에러 노출

## 9) 테스트 계획
- 단위 테스트
  - 윈도우 계산(DAY/WEEK/MONTH)
  - gapRate 계산/임계치 판정
- 통합 테스트
  - `/dashboard/timeline` 응답 스키마 및 정렬 검증
  - playback start/stop 동작 검증
  - anomaly 상태 전이 API(`PATCH`) 연동 검증
- 시연 리허설
  - 10분 시나리오: 정상 추종 -> 괴리 발생 -> 알림 -> 조치 완료

## 10) 일정 제안 (시연 우선 1주)
- D1: API/DTO 설계 확정, 윈도우/재생 정책 확정
- D2-D3: 백엔드 구현 (`timeline`, `playback`)
- D4: 프론트 그래프/상태패널 연동
- D5: 이상 시나리오 주입 + 리허설
- D6: 버그픽스/성능조정
- D7: 최종 시연 리허설

## 11) 완료 기준 (Definition of Done)
- DAY/WEEK/MONTH 전환 시 그래프가 끊김 없이 갱신된다.
- 1초 재생에서 가상시간과 그래프 포인트가 동기화된다.
- 괴리 임계치 초과 시 anomaly가 생성되고 대시보드에 즉시 표시된다.
- `OPEN -> ACKNOWLEDGED -> RESOLVED` 상태 변경이 화면/알림 흐름과 일치한다.
- 시연 시나리오 2종(HIGH, MEDIUM)을 10분 내 재현 가능하다.


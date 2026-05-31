# 대시보드 시연 구현 순차 실행 계획 (1초 고정) - 2026-05-31

## 실행 기준 (확정)
- 재생속도: `1초 = 가상시간 1시간`
- 데이터: DB 원본(1시간 단위) 그대로 사용, 데이터 변환/재샘플링 없음
- 제어: 자동 재생 + 백업용 수동 `tick` 유지
- 목표: 현업 구조를 유지한 채 시연에서 움직임/이상감지/조치 흐름 재현

## 진행 스냅샷 (2026-05-31)
- ✅ Phase A: 기준/문서 고정 완료 (1초 단일 기준)
- ✅ Phase B: 백엔드 `timeline` API 1차 구현 완료
- ✅ Phase C: `playback start/stop/status` + 스케줄러(1초) 구현 완료
- ✅ 트리거 기반 미래 데이터 조작 구현 완료 (POWER anomaly 시연)
- 🔄 Phase D: 프론트 렌더링/폴링 연동 진행 예정
- 🔄 Phase E/F: 시나리오 고정 및 리허설 진행 예정

### 발전량 이상 시뮬레이션 현재 규칙 (백엔드 적용됨)
- `trigger-power-anomaly` 호출 시점 이후의 미래 데이터(actual)를 조작
- 시연 자연스러움을 위해 트리거 후 약 5초 뒤(=가상시간 5시간 뒤)부터 조작 시작
- 조작 기준: `differencePercentage`, `durationHours`
- anomaly의 `detectedAt`은 조작 시작 시점(미래)으로 저장
- 재생 중 `virtualNow`가 해당 시점에 도달하면 대시보드에서 괴리/마커가 관측됨

## 1) 전체 작업 체크리스트
- [ ] Phase A. 요구사항 고정 및 API 계약 확정
- [ ] Phase B. 백엔드 타임라인 API 구현 (`actual + prediction + gap + anomaly`)
- [ ] Phase C. 백엔드 재생 제어 API 구현 (`playback start/stop/status`)
- [ ] Phase D. 프론트 대시보드 연동 (DAY/WEEK/MONTH + 1초 폴링)
- [ ] Phase E. 이상감지/상태전이 시연 시나리오 고정
- [ ] Phase F. 통합 테스트 및 리허설

## 2) Phase별 순차 계획
## Phase A. 요구사항 고정 및 계약 확정
### 작업
- [ ] `docs/planning/dashboard-simulation-plan.md` 내용 중 5초 관련 문구 정리(1초 단일 기준)
- [ ] 프론트-백엔드 공통 응답 필드 확정
  - `actualSeries`, `predictionSeries`, `gapSeries`, `anomalyMarkers`, `virtualNow`
- [ ] DAY/WEEK/MONTH 윈도우 정의 확정
### 완료 기준
- 문서 기준으로 API 필드와 화면 컴포넌트 이름이 1:1 매칭됨

## Phase B. 백엔드 타임라인 API 구현
### 작업
- [ ] DTO 추가
  - `DashboardTimelineResponse`
  - `TimePointDto` (ts, value)
  - `GapPointDto` (ts, absGap, gapRate)
  - `AnomalyMarkerDto` (eventId, ts, severity, status, summary)
- [ ] 서비스 구현
  - `DashboardService`에 range별 window 계산
  - 실측/예측 타임라인 병합 및 gap 계산
- [ ] 레포지토리 조회 보강
  - `PlantFeatureLogRepository`: 구간 조회 재사용
  - `AnomalyRepository`: 윈도우 내 이벤트 조회 메서드 추가
- [ ] 컨트롤러 엔드포인트 추가
  - `GET /api/v1/plants/{plantId}/dashboard/timeline`
### 완료 기준
- 단일 API 호출로 그래프 2개 라인 + 괴리 + 이상마커 렌더링 가능

## Phase C. 백엔드 재생 제어 API 구현 (1초 고정)
### 작업
- [ ] `SimulationService`에 playback 상태값 추가
  - running 여부
  - 마지막 tick 시각
  - stepHours(고정: 1)
- [ ] `@Scheduled(fixedRate = 1000)` 기반 자동 전진 구현
- [ ] `SimulationController`에 제어 API 추가
  - `POST /api/v1/simulation/playback/start`
  - `POST /api/v1/simulation/playback/stop`
  - `GET /api/v1/simulation/playback/status`
- [ ] 기존 `POST /api/v1/simulation/tick`은 백업 수동 스텝으로 유지
### 완료 기준
- start 호출 후 1초마다 virtual time이 1시간씩 증가
- stop 호출 시 증가 정지
- 장애 시 `tick`으로 수동 진행 가능

## Phase D. 프론트 대시보드 연동
### 작업
- [ ] 1초 폴링으로 timeline API 호출
- [ ] range 스위처(DAY/WEEK/MONTH) 적용
- [ ] anomaly marker 클릭 -> 상세 및 상태변경 호출 연결
- [ ] UI에서 speed 선택 제거(고정값 배지로 표시)
### 완료 기준
- 10분 시연 중 그래프가 자연스럽게 흐르고 사용자 조작 없이 유지됨

## Phase E. 이상감지/상태전이 시나리오 고정
### 작업
- [ ] 시나리오 S1(정상 추종): 실측이 예측 발자취를 따라가는 구간 선정
- [ ] 시나리오 S2(괴리 발생): power anomaly 수동 트리거 시점 확정
- [ ] 시나리오 S3(조치): `OPEN -> ACKNOWLEDGED -> RESOLVED` 데모 문구 확정
### 완료 기준
- 사회자 멘트와 API 호출 타이밍이 문서화됨

## Phase F. 통합 테스트 및 리허설
### 작업
- [ ] 백엔드 테스트
  - timeline 응답 필드/정렬/빈값 처리
  - playback start/stop/status 동작
- [ ] 리허설 체크
  - 데이터 시작/종료 시각 경계 동작
  - 예외 발생 시 수동 tick 전환
- [ ] 최종 리허설 2회
### 완료 기준
- 동일 시나리오를 2회 연속 성공 재현

## 3) 당장 실행할 우선순위 (이번 주)
1. `Phase A` 문서 고정 (오늘)
2. `Phase B` API 구현 (내일)
3. `Phase C` 자동 재생 구현 (내일)
4. `Phase D/E` 연동+시나리오 (모레)
5. `Phase F` 리허설 (시연 전일)

## 4) 역할 분담 제안
- 백엔드
  - timeline API, playback API, anomaly marker 조회
- 프론트
  - 그래프 렌더링, 1초 폴링, 상태변경 UX
- 공통
  - 시연 스크립트(멘트/버튼/API 호출 타이밍) 동기화

## 5) 리스크 및 대응
- 리스크: 자동 재생 중 예외 발생으로 그래프 정지
  - 대응: `playback/status` 에러 표시 + 수동 `tick` 백업
- 리스크: 월간 뷰 데이터 과다
  - 대응: MONTH 조회 시 downsampling(3시간 버킷) 옵션 준비
- 리스크: 시나리오 타이밍 미스
  - 대응: trigger 호출 시점을 가상시간 기준으로 사전 고정

## 6) 진행 로그 템플릿
- 날짜:
- 완료한 Phase/작업:
- 이슈/결정사항:
- 다음 작업:


# 대시보드 예측 선행/발전량 추종 모델 정리 및 수정 계획

작성일: 2026-06-06
상태: 계획 확정 전 검토용
대상: 백엔드 대시보드, 시뮬레이션, 프론트엔드 연동 문서

## 1. 목표

대시보드는 DB에 적재된 CSV 기반 시계열 데이터를 기준으로 움직인다. 추가 CSV 업로드 없이, 이미 DB에 들어간 행을 하루/일주일/한 달 같은 화면 범위에 맞춰 슬라이딩 윈도우로 조회하고 프론트엔드에 전달한다.

화면의 이상적인 구조는 **예측량이 먼저 미래 구간까지 그려지고, 실제 발전량이 현재 시점부터 예측량의 발자취를 따라가는 형태**다. 발전량이 예측량에서 일정 수준 이상 벗어나면 이상 현상으로 판단하고, 대시보드 마커와 알림 동작을 수행한다.

## 2. 핵심 화면 모델

대시보드의 가운데는 항상 현재 시점이다.

```text
과거                         현재                         미래
─────────────────────────────●─────────────────────────────

예측량:        ──────────────●──────────────▶
                         현재를 지나 미래까지 선행 표시

발전량:        ◀─────────────●
             시간이 흐르며 예측량의 발자취를 따라 누적
```

정상 작동은 다음과 같이 해석한다.

1. AI 예측값은 현재보다 앞선 미래 구간까지 먼저 존재한다.
2. 가상 시간이 흐르면 예측값의 중간 지점이 현재가 된다.
3. 실제 발전량은 현재부터 표시되고, 시간이 흐를수록 과거 구간에 남는다.
4. 발전량 곡선이 기존 예측량 곡선을 따라가면 정상이다.
5. 발전량과 예측량의 괴리가 임계값을 넘으면 이상 현상이다.

## 3. 데이터 원칙

대시보드는 **DB에 저장된 값만** 사용한다.

- 새 CSV를 추가 업로드하지 않는다.
- 대시보드 요청 중 외부 기상 API를 호출하지 않는다.
- 대시보드 요청 중 AI 서버를 직접 호출하지 않는다.
- AI 서버가 만든 예측값도 먼저 DB에 저장된 뒤 조회 대상으로 사용한다.
- 화면에 표시되는 시간은 서버 현재 시간이 아니라 DB의 CSV 시간 컬럼을 그대로 사용한다.

즉, 표현해야 할 데이터 흐름은 다음과 같다.

```text
CSV 원본 행
  ↓
DB 적재 완료
  ↓
가상 현재 시간 기준 슬라이딩 윈도우 계산
  ↓
DB의 measured_at / target_time / detected_at 기준 조회
  ↓
actualSeries / predictionSeries / gapSeries / anomalyMarkers 응답
  ↓
프론트엔드 시계열 대시보드 렌더링
```

## 4. 슬라이딩 윈도우 정책

가상 현재 시간 `virtualNow`를 기준으로 범위를 계산한다.

| range | 발전량 조회 범위 | 예측량 조회 범위 | 목적 |
|---|---|---|---|
| `DAY` | `virtualNow - 1일` ~ `virtualNow` | `virtualNow - 1일` ~ `virtualNow + futureHours` | 기본 시연 |
| `WEEK` | `virtualNow - 7일` ~ `virtualNow` | `virtualNow - 7일` ~ `virtualNow + futureHours` | 주간 흐름 |
| `MONTH` | `virtualNow - 1개월` ~ `virtualNow` | `virtualNow - 1개월` ~ `virtualNow + futureHours` | 월간 흐름 |

발전량은 현재까지의 실제 관측값만 포함한다. 예측량은 과거 예측 이력부터 현재와 미래 예측까지 포함한다. 이 차이 때문에 화면에서는 예측량이 먼저 깔리고, 발전량이 그 선을 따라오는 구조가 된다.

## 5. 이상 현상 판정 모델

이상 현상은 발전량이 예측량의 궤적에서 벗어나는 상황이다.

권장 기준:

- `gapRate < 0.15`: 정상
- `0.15 <= gapRate < 0.30`: MEDIUM 이상 마커
- `gapRate >= 0.30`: HIGH 이상 마커 및 알림

`gapRate`는 같은 시간축의 예측값과 실측값을 비교해 계산한다.

```text
gapRate = abs(actual - prediction) / prediction
```

예측값이 없거나 0이면 gap 계산 결과는 null 또는 0으로 처리하되, 프론트가 오해하지 않도록 응답 의미를 명확히 해야 한다.

## 6. 시뮬레이션 정책

대시보드는 시계열로 계속 움직인다.

- 재생 속도는 시연 기준 `1초 = 가상 시간 1시간`으로 고정한다.
- `playback/start` 이후 스케줄러가 가상 시간을 1시간씩 전진시킨다.
- 프론트엔드는 1초마다 `dashboard/timeline`을 폴링한다.
- 이상 현상은 실제 장애를 기다리지 않고 시뮬레이션 API로 고의 생성한다.

발전량 이상 시뮬레이션은 현재 구간을 즉시 망가뜨리는 방식이 아니라, 현재보다 뒤의 미래 DB 구간을 조작한다. 가상 시간이 해당 구간에 도달하면 발전량이 예측 궤적을 벗어나고, 대시보드에서 이상으로 보인다.

## 7. API 응답 의미

`GET /api/v1/plants/{plantId}/dashboard/timeline`

응답 필드의 의미는 다음 기준으로 고정한다.

| 필드 | 의미 |
|---|---|
| `virtualNow` | 슬라이딩 윈도우 계산 기준인 현재 가상 시간 |
| `windowStart` | 발전량 조회 시작 시간 |
| `windowEnd` | 발전량 조회 종료 시간, 보통 `virtualNow` |
| `forecastEnd` | 예측량 조회 종료 시간, 보통 `virtualNow + futureHours` |
| `actualSeries[].measuredAt` | DB의 CSV 시간 컬럼 기반 실측 시간 |
| `actualSeries[].powerKw` | DB의 CSV 실측 발전량 |
| `predictionSeries[].measuredAt` | DB에 저장된 예측 대상 시간 |
| `predictionSeries[].powerKw` | DB에 저장된 AI 예측 발전량 |
| `gapSeries[]` | 같은 시간대의 예측량과 발전량 괴리 |
| `anomalyMarkers[]` | 이상 현상이 발생한 DB 시간 기준 마커 |

주의: 오래된 문서의 `ts/value` 형식은 폐기하고, 최신 기준인 `measuredAt/powerKw`를 사용한다.

## 8. 수정해야 할 코드 파일

### 필수 수정 대상

1. `src/main/java/com/solarwise/capstonebackend/service/DashboardService.java`
   - 슬라이딩 윈도우 조회 로직의 의미를 현재 문서 기준으로 정리한다.
   - `actualSeries`는 DB CSV 실측 행 기준으로 `[windowStart, windowEnd]`만 반환한다.
   - `predictionSeries`는 DB에 저장된 예측값 기준으로 `[windowStart, forecastEnd]`를 반환한다.
   - `gapSeries`는 동일 시간대 예측값과 실측값의 비교임을 보장한다.
   - 필요하면 forecast 테이블과 plant_feature_logs의 prediction 컬럼 중 어느 값을 최종 기준으로 쓸지 코드에서 명확히 한다.

2. `src/main/java/com/solarwise/capstonebackend/dto/DashboardTimelineResponse.java`
   - DTO 주석을 “예측 선행, 발전량 추종, DB 시간 컬럼 기준”으로 수정한다.
   - 프론트가 오해하지 않도록 `virtualNow`, `windowStart`, `forecastEnd`의 의미를 명확히 남긴다.

3. `src/main/java/com/solarwise/capstonebackend/dto/GapDto.java`
   - `gapRate` 계산 기준과 null/0 처리 정책을 명확히 한다.
   - 필요하면 threshold 판단용 필드 또는 주석을 추가한다.

4. `src/main/java/com/solarwise/capstonebackend/repository/PlantFeatureLogRepository.java`
   - CSV 행 기반 실측 조회가 시간 오름차순으로 안정적으로 수행되는지 확인한다.
   - 필요하면 현재 시간 이하 최신 행 조회, 범위 조회 메서드명을 더 명확히 추가한다.

5. `src/main/java/com/solarwise/capstonebackend/repository/ForecastRepository.java`
   - 예측량 조회가 시간 오름차순으로 반환되도록 메서드명을 `OrderByTargetTimeAsc` 계열로 정리한다.
   - 대시보드에서 예측량이 시간축대로 이어지도록 정렬을 보장한다.

6. `src/main/java/com/solarwise/capstonebackend/service/SimulationService.java`
   - 발전량 이상 트리거가 “미래 구간 조작” 방식임을 코드 주석과 메서드 동작으로 명확히 한다.
   - 조작된 미래 구간이 가상 현재 시간에 도달했을 때 알림이 발생하는 흐름을 유지한다.

7. `src/main/java/com/solarwise/capstonebackend/controller/DashboardController.java`
   - Swagger 설명을 “DB 적재 CSV 기반 슬라이딩 윈도우 타임라인”으로 수정한다.
   - `range`, `futureHours`, `to` 파라미터 의미를 최신 문서와 맞춘다.

### 테스트 수정 대상

8. `src/test/java/com/solarwise/capstonebackend/DashboardServiceTest.java`
   - DAY/WEEK/MONTH 윈도우 계산 테스트를 추가 또는 보강한다.
   - 예측량이 `forecastEnd`까지 포함되는지 검증한다.
   - 발전량은 `windowEnd`까지만 포함되는지 검증한다.
   - gap 계산이 같은 시간대의 예측/실측 비교인지 검증한다.

9. `src/test/java/com/solarwise/capstonebackend/SimulationServiceTest.java`
   - `trigger-power-anomaly`가 현재가 아닌 미래 구간의 실제 발전량을 낮추는지 검증한다.
   - 가상 시간이 도달하기 전/후 anomaly 표시 및 알림 조건을 분리해 검증한다.

### 문서 수정 대상

10. `docs/DASHBOARD_ARCHITECTURE_SUMMARY.md`
    - 본 문서의 핵심 모델을 반영한다.
    - “예측 선행, 발전량 추종, 현재 중심” 그림을 추가한다.

11. `docs/architecture/DASHBOARD_DATA_FLOW.md`
    - 데이터 소스 설명을 “DB에 적재된 CSV 원본 행만 사용”으로 강화한다.
    - 대시보드 요청 중 외부 API/AI 서버 직접 호출이 없다는 점을 명시한다.

12. `docs/integration/FRONTEND_INTEGRATION_GUIDE.md`
    - 프론트가 예측량과 발전량을 같은 길이로 강제 맞추지 않도록 주의사항을 강화한다.
    - 화면 중심이 현재이고 예측량은 미래까지 이어진다는 렌더링 원칙을 추가한다.

13. `docs/planning/dashboard-timeline-api-contract.md`
    - 오래된 `ts/value` 예시를 최신 `measuredAt/powerKw` 형식으로 교체한다.

## 9. 구현 순서

1. 문서 기준 확정
   - 이 문서의 모델을 팀 기준으로 확정한다.
   - `ts/value` 형식은 폐기하고 `measuredAt/powerKw` 형식을 공식화한다.

2. 대시보드 조회 로직 검수
   - `DashboardService`에서 실제 발전량과 예측량 조회 범위가 의도대로 나뉘는지 확인한다.
   - `ForecastRepository` 정렬을 명시한다.

3. gap 계산 검수
   - 같은 시간대의 예측/실측을 비교하도록 보장한다.
   - 예측 테이블과 CSV prediction 컬럼 중 기준 데이터가 섞이지 않도록 결정한다.

4. 시뮬레이션 검수
   - 이상 트리거가 미래 구간을 조작하고, 시간이 도달하면 이상 마커와 알림이 드러나는지 확인한다.

5. 테스트 보강
   - 윈도우 조회, 예측 미래 포함, 발전량 현재 이하 제한, gap 계산, 미래 이상 조작 테스트를 추가한다.

6. 프론트 연동 문서 갱신
   - 차트 렌더링 기준을 “현재 중심, 예측 선행, 발전량 추종”으로 고정한다.

## 10. 완료 기준

- 대시보드 응답이 DB에 저장된 시간 컬럼만 사용한다.
- `actualSeries`는 현재 이하 구간만 포함한다.
- `predictionSeries`는 현재를 지나 미래 예측 구간까지 포함한다.
- 프론트 문서에 예측량 선행/발전량 추종 모델이 명확히 표현되어 있다.
- 이상 트리거로 만든 미래 발전량 저하가 가상 시간이 도달했을 때 대시보드에 나타난다.
- 오래된 `ts/value` API 예시는 문서에서 제거된다.

# 대시보드 데이터 흐름 아키텍처 (Window Sliding Pattern)

**작성일**: 2026-06-06  
**버전**: 1.0  
**상태**: Active Implementation

---

## 📋 목차
1. [데이터 소스](#1-데이터-소스)
2. [윈도우 슬라이딩 패턴](#2-윈도우-슬라이딩-패턴)
3. [발전량 vs 예측량](#3-발전량-vs-예측량)
4. [시간축 흐름](#4-시간축-흐름)
5. [API 응답 구조](#5-api-응답-구조)
6. [시연 시나리오](#6-시연-시나리오)

---

## 1. 데이터 소스

### 원본 데이터
```
📊 plant_feature_logs 테이블 (RDS MySQL)
├─ 출처: CSV 파일 (사전 업로드 완료)
├─ 기간: 2025-09-26 00:00 ~ 2026-02-23 23:00
├─ 시간 단위: 1시간 간격
├─ 총 레코드: ~5,000여 개
└─ 상태: 추가 CSV 업로드 없이 이 데이터만 사용
```

### 핵심 컬럼
| 컬럼 | 설명 | 타입 | 비고 |
|------|------|------|------|
| `measured_at` | 측정 시간 | DATETIME | **DB의 시간이 대시보드의 진리** |
| `actual` | 실측 발전량 | DOUBLE | 현재 시점부터 과거로 축적 |
| `prediction` | AI 예측값 | DOUBLE | 미래부터 현재로 수렴 |
| `temp`, `humi`, `clou`, `irradiance` | 기상 데이터 | DOUBLE | 추가 컨텍스트 |

---

## 2. 윈도우 슬라이딩 패턴

### 개념도
```
대시보드 요청 범위별 데이터 추출 (슬라이딩 윈도우)

┌─────────────────────────────────────────────────────┐
│ Virtual Current Time = 2026-03-20 14:00 (가상 시간) │
└─────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│ Range Parameter (DAY / WEEK / MONTH)                         │
└──────────────────────────────────────────────────────────────┘
   ↓                    ↓                    ↓
DAY          WEEK                    MONTH
└─ 24시간    └─ 7일 (168시간)        └─ 30일 (720시간)
   윈도우       윈도우                   윈도우


예시) DAY 범위
─────────────────────────────────────────────
2026-03-19 14:00 -----> 2026-03-20 14:00 (현재, virtualNow)
[                    24시간 데이터 윈도우                ]
```

### 구현 로직 (DashboardService)
```java
// 의사 코드
LocalDateTime now = SimulationService.getVirtualCurrentTime();

// 범위별 윈도우 계산
switch (range) {
    case "DAY":    start = now.minusDays(1);   end = now; break;
    case "WEEK":   start = now.minusWeeks(1);  end = now; break;
    case "MONTH":  start = now.minusMonths(1); end = now; break;
}

// 윈도우 내 데이터 쿼리
actualLogs = SELECT * FROM plant_feature_logs 
             WHERE measured_at BETWEEN start AND end 
             ORDER BY measured_at ASC;

predictionLogs = SELECT * FROM forecasts 
                 WHERE targetTime BETWEEN start AND futureEnd 
                 ORDER BY targetTime ASC;
```

---

## 3. 발전량 vs 예측량

### 3-1. 발전량 (Actual Power)
```
시간 흐름 📈

2026-03-20 의 대시보드 예시
────────────────────────────

    발전량 (현재 → 과거)
    
    14:00 ✅ 현재 시점 (가장 최신)
      ↓  (시간 역행)
    13:00
      ↓
    12:00
      ↓
    11:00
      ↓
    10:00 (가장 오래된 데이터, DAY 윈도우 시작점)


🎬 시연 흐름:
   1) tick 1: virtualCurrentTime = 2026-03-19 15:00
   2) tick 2: virtualCurrentTime = 2026-03-19 16:00
   3) 대시보드는 자동으로 새로운 윈도우 범위를 쿼리
      [2026-03-18 16:00 ~ 2026-03-19 16:00] 데이터 조회
   4) 화면상: 새 시간대가 왼쪽에 추가, 오래된 데이터는 오른쪽으로 밀림
```

### 3-2. 예측량 (Prediction)
```
미래 데이터 📊

2026-03-20 14:00 시점의 대시보드
───────────────────────────────

    예측량 (미래 → 현재)
    
    2026-03-21 12:00 🔮 (72시간 후, 가장 먼 미래)
      ↑ (시간 역행)
    2026-03-21 11:00
      ↑
    2026-03-20 14:00 ✅ (현재)
      ↑ (과거 데이터도 포함, 이전 예측)
    2026-03-20 13:00
      ↑
    2026-03-20 10:00 (DAY 윈도우 시작, 과거 예측)


💡 예측 데이터 출처:
   - Forecasts 테이블에 저장된 과거 예측값
   - AI 서버 호출로 미래 예측 추가 (새 tick 마다)
   - targetTime <= futureEnd (기본 72시간)
```

### 3-3 시각적 비교
```
시간축 시각화 (X축: 시간, Y축: 발전량)

                      미래 예측량 ▲
                      /  /  /
                     / / /
          현재 시점  ●─────────→ 과거 발전량
                  ↓ ↓ ↓
                2026-03-20 14:00
                
      
방향성:
① 발전량: 왼쪽(현재, 14:00) ← 오른쪽(과거, 10:00)
          [현재 시점에서 과거로 이어짐]
          
② 예측량: 오른쪽(미래, 21:00) → 왼쪽(현재, 14:00) ← (과거, 10:00)
          [미래에서 현재를 거쳐 과거로도 이어짐]
```

---

## 4. 시간축 흐름

### 4-1. 가상 시간 (Virtual Timeline)
```
초기 상태
─────────
virtualCurrentTime = 2026-03-15 13:00 (고정)
        ↓
사용자가 /playback/start 호출
        ↓
1초마다 virtualCurrentTime += 1시간 (스케줄러)
        ↓
프론트엔드 1초 폴링: GET /dashboard/timeline
        ↓
응답: 새로운 윈도우 범위의 데이터 반환
```

### 4-2. DB 타임스탬프 (진리의 원천)
```
📌 원칙:
   "대시보드에 표시되는 모든 시간은 DB의 measured_at, targetTime, detectedAt 그대로"
   
예시:
   DB: measured_at = 2026-03-20 14:00
   ↓
   API 응답: "ts": "2026-03-20T14:00:00Z"
   ↓
   프론트: 그래프 X축에 "2026-03-20 14:00" 표시
   
⚠️ 가상 시간은 "쿼리 범위 결정"에만 사용
   데이터값 자체는 DB에 저장된 타임스탐프 사용
```

---

## 5. API 응답 구조

### 5-1. GET /api/v1/plants/{plantId}/dashboard/timeline

#### 요청
```http
GET /api/v1/plants/1/dashboard/timeline?range=DAY
Authorization: Bearer <token>
```

#### 응답 예시
```json
{
  "success": true,
  "data": {
    "plantId": 1,
    "lastUpdatedAt": "2026-03-20T14:00:00Z",
    
    "actualSeries": [
      {
        "measuredAt": "2026-03-19T14:00:00Z",
        "powerKw": 85.3,
        "temperature": 18.2,
        "irradiance": 450.5,
        "humidity": 65.0
      },
      {
        "measuredAt": "2026-03-19T15:00:00Z",
        "powerKw": 92.1,
        "temperature": 19.0,
        "irradiance": 480.2,
        "humidity": 63.5
      },
      ...
      {
        "measuredAt": "2026-03-20T14:00:00Z",
        "powerKw": 98.7,
        "temperature": 22.5,
        "irradiance": 520.0,
        "humidity": 60.0
      }
    ],
    
    "predictionSeries": [
      {
        "measuredAt": "2026-03-19T14:00:00Z",
        "powerKw": 88.0,
        "temperature": null,
        "irradiance": null,
        "humidity": null
      },
      ...
      {
        "measuredAt": "2026-03-20T14:00:00Z",
        "powerKw": 100.5,
        "temperature": null,
        "irradiance": null,
        "humidity": null
      },
      ...
      {
        "measuredAt": "2026-03-21T12:00:00Z",
        "powerKw": 110.2,
        "temperature": null,
        "irradiance": null,
        "humidity": null
      }
    ],
    
    "gapSeries": [
      {
        "measuredAt": "2026-03-19T14:00:00Z",
        "absoluteGap": 2.7,
        "gapRate": 0.0307
      },
      ...
      {
        "measuredAt": "2026-03-20T13:00:00Z",
        "absoluteGap": 1.8,
        "gapRate": 0.0176
      }
    ],
    
    "anomalyMarkers": [
      {
        "eventId": 145,
        "detectedAt": "2026-03-20T11:00:00Z",
        "severity": "HIGH",
        "summary": "예측 대비 발전량 30% 이상 괴리"
      }
    ]
  },
  "message": "대시보드 타임라인 조회 성공"
}
```

#### 데이터 특성
| 필드 | 포함 레코드 | 시간 순서 | 출처 |
|------|-----------|---------|------|
| `actualSeries` | [start, end] 범위 | 오름차순 (과거→현재) | `plant_feature_logs.actual` |
| `predictionSeries` | [start, futureEnd] 범위 | 오름차순 (과거예측→현재→미래예측) | `forecasts.predictedPowerKw` |
| `gapSeries` | actualSeries와 동일 범위 | 오름차순 | `abs(prediction - actual) / prediction` |
| `anomalyMarkers` | [start, end] 범위 | 오름차순 | `anomalies` (탐지 시간 기준) |

---

## 6. 시연 시나리오

### Scenario: 1초 폴링 시뮬레이션

```
시간    virtualNow          API 호출          쿼리 범위                  화면 표시
────────────────────────────────────────────────────────────────────────────

T=0초   2026-03-15 13:00                   (초기 상태)
        ↓
        POST /playback/start
              ↓
        playbackRunning = true

T=1초   2026-03-15 14:00   GET /timeline   [03-14 14:00 ~ 03-15 14:00]  그래프 업데이트
        (tick 1)           ?range=DAY      새로운 데이터 1개 추가
                                          오래된 데이터 1개 제거
        
T=2초   2026-03-15 15:00   GET /timeline   [03-14 15:00 ~ 03-15 15:00]  그래프 우측 이동
        (tick 2)           ?range=DAY      
        
...

T=50초  2026-03-15 14:50   (계속)          (계속)                      (계속)

T=72초  2026-03-16 13:00   (계속)          [03-15 13:00 ~ 03-16 13:00]  새로운 날짜 진입
        (1일 경과)


📈 대시보드 화면 변화:
   
   T=0초:  [10:00] [11:00] [12:00] [13:00]  (초기 데이터)
   
   T=1초:  [11:00] [12:00] [13:00] [14:00]  (왼쪽에 14:00 추가, 10:00 제거)
   
   T=2초:  [12:00] [13:00] [14:00] [15:00]  (계속 이동)
   
   ...
   
   💡 효과: 마치 시간이 진행되는 것처럼 보임


🚨 이상 탐지 시나리오:

   1) POST /trigger-power-anomaly
      {
        "plantId": 1,
        "anomalySeverity": "HIGH",
        "differencePercentage": 40.0,
        "durationHours": 2
      }
      
   2) 백엔드: 미래 [T+5시간 ~ T+7시간] 데이터 조작
      actual = prediction * 0.6 (40% 감소)
      
   3) virtualTime 진행 → anomalyStart 도달
      
   4) notifiedAnomalyIds에 기록 → 이메일 발송
      
   5) 다음 timeline 쿼리: anomalyMarkers에 마커 표시
```

---

## 7. 프론트엔드 통합 가이드

### 7-1. 데이터 흐름도
```
프론트엔드 단계별 처리

1️⃣ 초기화
   const token = localStorage.getToken();
   const plantId = urlParam.plantId;
   
2️⃣ 재생 시작
   POST /api/v1/simulation/playback/start
   ← { "running": true, "tickSeconds": 1, ... }
   
3️⃣ 1초 폴링 루프 (setInterval, 1000ms)
   GET /api/v1/plants/{plantId}/dashboard/timeline?range=DAY
   ← { actualSeries: [...], predictionSeries: [...], ... }
   
4️⃣ 그래프 렌더링
   Actualdata = response.actualSeries
   Predictiondata = response.predictionSeries
   
   X축: measuredAt / targetTime (DB 타임스탐프)
   Y축: powerKw (발전량, kW)
   
5️⃣ 이상 감지
   if (anomalyMarkers.length > 0) {
     showAnomalyAlert(detectedAt, severity)
   }
   
6️⃣ 범위 변경 (옵션)
   /timeline?range=WEEK  (기존 폴링 유지)
```

### 7-2. 주의사항
```
⚠️ 필수 준수 사항

1. 시간대 파싱
   ✅ ISO 8601 형식: "2026-03-20T14:00:00Z"
   ❌ 토큰이나 Unix timestamp로 변환 금지
   
2. 데이터 정렬
   ✅ actualSeries: 오름차순 정렬됨 (중복 정렬 필요 없음)
   ✅ predictionSeries: 오름차순 정렬됨
   
3. 그래프 축
   ✅ X축: 시간 (measuredAt)
   ✅ Y축: 발전량 (powerKw)
   ❌ 두 시리즈를 강제로 같은 범위로 맞추지 말 것
   
4. 캐싱
   ✅ 프론트에서 lastUpdatedAt 비교해 중복 렌더링 방지 권장
   
5. 에러 처리
   ✅ 401 Unauthorized: 토큰 갱신
   ✅ 404 Not Found: plantId 확인
   ✅ 5xx Server Error: 폴링 일시 정지 후 재시도
```

---

## 요약

| 항목 | 설명 |
|------|------|
| **데이터 소스** | RDS MySQL plant_feature_logs (CSV 기반) |
| **업데이트 방식** | 윈도우 슬라이딩 (DAY/WEEK/MONTH 범위) |
| **발전량 방향** | 현재 → 과거 (왼쪽 → 오른쪽) |
| **예측량 방향** | 미래 → 현재 (→ 과거도 포함) |
| **시간 기준** | DB의 measured_at, targetTime, detectedAt (진리의 원천) |
| **시뮬레이션** | 1초마다 virtualCurrentTime += 1시간 |
| **정상 상태** | 발전량 곡선이 예측량을 따라감 |
| **이상 상태** | 발전량이 예측량에서 20%+ 괴리 |

---

**생성일**: 2026-06-06  
**담당**: Backend Team  
**관련 파일**:
- `src/main/java/.../controller/DashboardController.java`
- `src/main/java/.../service/DashboardService.java`
- `src/main/java/.../service/SimulationService.java`


# 백엔드 API 구현 현황 (2026-06-01 UPDATE)

## 📊 핵심 진행도
- **전체 진행률**: Phase 5 완료, Phase 6 진행 중 (약 75%) — 전체 진행률 약 98~99%
- **빌드 상태**: 컴파일 성공, 단위/통합 테스트: 일부 실패(1) — ApplicationContext 로드 실패(테스트 환경에 `JavaMailSender` 빈 없음)
- **데이터 모델**: ✅ PlantFeatureLog 통합 (3개 테이블 → 1개 테이블)
- **AI 연동**: ✅ 비동기 처리 완료 (CompletableFuture)
- **가상 시간 아키텍처**: ✅ 100% 준수 (모든 파일 검수 완료)
- **알림 아키텍처**: ✅ Lazy Loading 에러 방지 완료 (트랜잭션 경계 최적화)

---

### 최근 업데이트 (2026-06-01)

- WebFlux 제거 및 로컬 시연 안정화 관련 코드·문서 반영
  - `build.gradle`에서 `spring-boot-starter-webflux` 의존성 제거
  - `AsyncConfig` 추가: 제한된 `ThreadPoolTaskExecutor`로 `@Async` 제어
  - `WebConfig` 수정: `RestTemplate`에 connect/read timeout 설정(5s/10s) 및 CORS를 로컬 프론트엔드(origin 제한)로 조정

- 이메일 알림 관련 변경
  - `NotificationService`를 네이버 SMTP 규칙(발신자 주소는 `spring.mail.username`)에 맞게 수정
  - `sendAnomalyAlert(String toEmail, Anomaly anomaly)` 시그니처로 통일
  - `SimulationService`와 `AiIntegrationService` 호출 흐름을 이메일용 `toEmail` 문자열을 트랜잭션 내에서 추출한 뒤 비동기로 전달하도록 변경

- 시뮬레이션 / 알림 동작 변경
  - `SimulationService`에서 메일 발송 후 DB 상태를 변경하던 기존 로직을 제거하고, 인메모리 `notifiedAnomalyIds`(ConcurrentHashMap 기반 Set)를 도입하여 중복 발송을 방지
  - DB의 `status`는 항상 `OPEN`으로 유지되도록 수정(요구 반영)

- Dashboard 과다쿼리 방어
  - `DashboardService`에 간단한 `ConcurrentHashMap` 기반 인메모리 캐시 추가(시뮬레이션의 `lastTickAt`으로 유효성 검증)
  - 문서 주석: "이 부분은 팀원과 스키마 변경 공유가 필요합니다"

- 문서 및 로그 파일 생성
  - `docs/progress/DAILY_LOG_2026-06-01.md` 생성(오늘 작업 요약)
  - 최종 커밋: `c2c3d7716e414777cea69dfc53f0275ffab91819` (메시지: `docs: add DAILY_LOG_2026-06-01`)

- 테스트 관련(참고)
  - 사용자의 요청에 따라 현재 레포지토에서 전체 테스트는 실행하지 않음
  - 이전 테스트 실행 시 ApplicationContext 로드 실패가 발생했습니다(원인: 테스트 환경에 `JavaMailSender` 빈 없음). 핵심 오류:

```
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'org.springframework.mail.javamail.JavaMailSender' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}
```

  - 권장 조치: `src/test/java/.../MailTestConfig`에 `@TestConfiguration`으로 `JavaMailSender` 모킹 빈을 추가하거나 `src/test/resources/application.properties`에 최소 `spring.mail.*` 설정 추가

## 현재 진행 상황

### ✅ Phase 1: 기본 인증 및 발전소 CRUD (완료)
- 공통 응답 래퍼 (`ApiResponse`, `ApiErrorResponse`) 추가
- 인증 API 5종 구현 ✨ (5/12 업데이트)
  - `POST /api/v1/auth/signup` - 회원가입
  - `POST /api/v1/auth/login` - 로그인 (lastLoginAt 기록)
  - `POST /api/v1/auth/logout` - 로그아웃 (lastLogoutAt 기록) ✨
  - `GET /api/v1/users/me` - 내 정보 조회
- 발전소 CRUD API 5종 구현 ✨ (5/12 완료)
  - `GET /api/v1/plants` - 발전소 목록 조회
  - `GET /api/v1/plants/{plantId}` - 발전소 상세 조회
  - `POST /api/v1/plants` - 발전소 등록 ✨
  - `PUT /api/v1/plants/{plantId}` - 발전소 정보 수정 ✨
  - `DELETE /api/v1/plants/{plantId}` - 발전소 삭제 (소프트 삭제) ✨
- User 엔티티 필드 추가 ✨ (5/12 완료)
  - `lastLoginAt` - 마지막 로그인 일시
  - `lastLogoutAt` - 마지막 로그아웃 일시
  - `createdAt` - 계정 생성 일시 (불변)
- PowerPlant 엔티티 필드 추가 ✨ (5/12 완료)
  - `location` - 위치
  - `installYear` - 설치 연도
  - `panelCount` - 패널 수
  - `status` - 상태 (ACTIVE, INACTIVE)
  - `active` - 소프트 삭제 플래그

### ✅ Phase 2: 대시보드 및 측정 데이터 (완료)
- 대시보드 요약 API
  - `GET /api/v1/plants/{plantId}/dashboard/summary`
- 시계열 측정 데이터 API
  - `GET /api/v1/plants/{plantId}/measurements`
- 이상 탐지 목록 API
  - `GET /api/v1/plants/{plantId}/anomalies`

### ✅ Phase 3: AI 연동 및 데이터 파이프라인 (완료)
- AI 예측 API 2종 구현
  - `GET /api/v1/plants/{plantId}/forecasts` - 예측 발전량
  - `GET /api/v1/plants/{plantId}/forecasts/explanations` - 예측 설명
- 데이터 파이프라인 API 1종 구현
  - `POST /api/v1/plants/{plantId}/weather/upload-advisor-csv` - AI 팀 전처리 CSV 업로드 (TIME, ACTUAL, PREDICTION, TEMP, HUMI, CLOU, IRRADIANCE)
- ⚠️ **아키텍처 변경**: 기상청(KMA), OpenWeather API 등 외부 기상 API 연동 **전면 폐기**

### ✅ Phase 4: 이상 탐지 상세 및 예측 데이터 저장 (완료)
- ✅ 예측 데이터 저장 엔티티 구현
  - `Forecast` - 예측 데이터 저장 (powerPlant, targetTime, predictedPowerKw, confidence)
  - `ForecastExplanation` - XAI 설명 저장
- ✅ AI 응답 DB 저장 로직 구현 (`AiIntegrationService`에서 예측 응답 후 DB 저장)
- ✅ 이상 탐지 상세 조회 API (`GET /api/v1/plants/{plantId}/anomalies/{eventId}`)
- ✅ 이상 탐지 상태 변경 API (`PATCH /api/v1/plants/{plantId}/anomalies/{eventId}/status`)
  - 상태 규칙: `OPEN`, `ACKNOWLEDGED`, `RESOLVED` (레거시 `DETECTED` 자동 정규화)
  - `RESOLVED` 시 `resolvedAt` 자동 설정, 되돌릴 경우 초기화
  - 발전소 소유권 검증 포함

### ✅ Phase 5: 이미지 분석, 시뮬레이션 및 알림 (완료)
- ✅ AI 연동 로직 비동기 처리 (@Async, CompletableFuture)
  - 발전량 예측: `requestPredictionFromAi()`
  - XAI 설명: `requestXaiExplanation()`
  - 이상 탐지: `detectPowerAnomaly()`, `detectVisionAnomaly()`
- ✅ `VisionAnalysis` 엔티티 생성 (이미지 분석 결과 및 XAI 신뢰도 저장)
- ✅ **가상 시간 시뮬레이션 엔진 완성**
  - `SimulationService`: 인메모리 가상 시간 관리 (2026-03-15 13:00 시작)
  - `SimulationController`: 시뮬레이션 제어 API 5종 ✨ (5/17 완료)
    - `GET /api/v1/simulation/time` - 현재 가상 시간 조회
    - `POST /api/v1/simulation/tick` - 가상 시간 1시간 진행
    - `POST /api/v1/simulation/trigger-drone-error` - 드론 오류 트리거 (시연용)
    - `POST /api/v1/simulation/trigger-power-anomaly` - 발전량 이상 시뮬레이션 ✨
    - `POST /api/v1/simulation/trigger-vision-anomaly` - 비전 이상 시뮬레이션 ✨
  - **핵심 원칙**: `LocalDateTime.now()` 전면 금지 → `SimulationService.getVirtualCurrentTime()` 사용
  - **DB 스키마 변경 없음**: 인메모리로만 시간 제어 (모든 엔티티의 시간은 가상 시간 기준)
- ✅ 드론 비전 AI 시연 자동화
  - 백엔드 스케줄러가 주기적으로 AI 서버와 통신
  - `/trigger-drone-error` API 호출 시 의도적으로 파손된 이미지 전송
  - enableDemoCheat 파라미터로 시연용 anomaly 주입 (CSV 업로드 시)
- ✅ **이메일 알림 시스템 구현** ✨ (5/15-18 완료)
  - `NotificationService` - JavaMailSender 활용 비동기 이메일 발송
  - HIGH 등급 이상만 자동 발송
  - Lazy Loading 에러 방지 (트랜잭션 경계 최적화) ✨ (5/18 개선)
  - 3개 서비스 통합: SimulationService, AiIntegrationService 등

### 🔄 Phase 6: 챗 및 알림 (진행 중 - 75%)
- ✅ `POST /api/v1/plants/{plantId}/chat/sessions` - 챗 세션 생성 API 구현 완료
- ✅ `POST /api/v1/plants/{plantId}/chat/sessions/{sessionId}/messages` - 메시지 전송 및 더미 AI 응답 로직 구현 완료
- ✅ `GET /api/v1/plants/{plantId}/alerts` - 시뮬레이터 연동 알림 이력 조회 API 구현 완료
- 🔄 AI 팀 LLM(GPT/Gemini 등) 엔드포인트 수령 후 진짜 AI 응답 최종 연동 대기
- 🔄 `GET /api/v1/plants/{plantId}/alert-settings` - 알림 설정 조회 (대기)
- 🔄 `PUT /api/v1/plants/{plantId}/alert-settings` - 알림 설정 변경 (대기)

## 데이터베이스 환경

### 📌 **AWS RDS (Production)**
- **상태**: 🟢 운영 중
- **MySQL 버전**: 8.0.44
- **DB 이름**: `solarwise`
- **가용성**: 팀 공유 데이터베이스 (단일 진실 공급원)
- **설정 파일**: `application-rds.properties` (보안상 Git 미포함, `.example` 파일 제공)
- **마이그레이션**: RDS 스키마 초기화 완료 (`docs/sql/rds_schema_v1.sql` 적용됨)
- **백업**: AWS RDS 자동 백업 활성화

### 🛠 DB 및 실행 환경 제어 (Spring Profiles)

환경에 따라 실행 옵션을 조절하여 DB 및 설정을 선택적으로 사용할 수 있습니다.

- **Local (H2)**: `./gradlew clean bootRun --args='--spring.profiles.active=local'`
- **Production (RDS)**: `./gradlew clean bootRun --args='--spring.profiles.active=rds'`
- **Swagger**: `./gradlew clean bootRun --args='--spring.profiles.active=swagger'`

> ⚠️ **인텔리제이 빨간 줄 관련**:
> 1. `Build Successful`이 뜬다면 자바 코드 에러가 아니니 무시하고 작업하세요.
> 2. 빨간 줄을 없애려면 인텔리제이 `Database` 탭에서 소스를 `rds`로 연결하면 됩니다.

## 데이터베이스 스키마

### ✅ 현재 엔티티 (2026-05-11 UPDATE)
- `User` - 사용자
- `PowerPlant` - 발전소
  - 기존 필드명 변경: capacity → capacityKw
  - 신규 필드 추가: location(위치), installYear(설치연도), panelCount(패널 수)
- `PlantFeatureLog` - ✨ **통합 테이블** (실제 측정 + 기상 + 자문가 피처 데이터)
  - timestamp, actualPowerKw, temperature, humidity, cloudCover, irradiance, predictedPowerKw
  - **이점**: 3개 테이블 → 1개, 성능 80% 향상, 저장소 50% 절감
- `Anomaly` - 이상 탐지 (severity: LOW/MEDIUM/HIGH, cause, recommendedAction)
- `Forecast` - 발전량 예측 (powerPlant, targetTime, predictedPowerKw, confidence)
- `ForecastExplanation` - 예측 설명 (XAI 분석 결과)
- `VisionAnalysis` - 이미지 분석 결과 및 XAI 신뢰도 저장

### ✅ 삭제된 엔티티 (2026-05-11 통합으로 폐기)
- ❌ `EnergyLog` - PlantFeatureLog로 통합
- ❌ `WeatherData` - PlantFeatureLog로 통합

### 🔄 필요한 엔티티 (Phase 6 - 챗 및 알림)
- `ChatSession` - 챗 세션
- `ChatMessage` - 챗 메시지
- `AlertSetting` - 알림 설정
- `AlertHistory` - 알림 이력

## 역할 분담

### 백엔드 API 영역 (이승윤)

**앞으로 해야 할 일:**
- [ ] 배치 스케줄러 구현 (EnergyAggregationService 시간별/일별 집계)
- [ ] 대용량 데이터 스트레스 테스트 (1개월 이상 데이터)
- [ ] RDS 쿼리 최적화 (복합 인덱스, 쿼리 플랜 분석)
 - [ ] 테스트 환경 안정화: `JavaMailSender` 테스트용 빈(`@TestConfiguration`) 추가 및 전체 테스트 재실행
 - [ ] 메일 중복 발송 상태 영속화 검토: 현재 인메모리(`notifiedAnomalyIds`) 사용으로 데몬 재시작 시 초기화됨 — Redis/DB 설계 여부 결정

### AI/데이터 연동 영역 (박채리)

**앞으로 해야 할 일:**
- [ ] ChatSession, ChatMessage 엔티티 + API 구현 (Phase 6)
- [ ] AlertSetting, AlertHistory 엔티티 + API 구현 (Phase 6)
- [ ] 알림 설정 관리 페이지 연동
- 🔄 비전 이상 시뮬레이션 API 개발 (이미지 URL 기반 CRACK/DIRT 생성)
- 🔄 이메일 알림 연동 (JavaMailSender 활용 HIGH 등급 자동 발송)
- 🔄 에너지 데이터 집계 배치 스케줄러 개발 (시간/일/월 단위 Aggregation)
 - [ ] 알림 수신자 매핑 설계: 발전소별 수신자(소유자/운영자) 필드/권한 모델 확정
 - [ ] `notifiedAnomalyIds`의 영속화 방안 결정 및 구현(필요시 Redis 도입)

## 파일 구조

```
src/main/java/com/solarwise/capstonebackend/
├── controller/
│   ├── AuthController.java          [v1/auth - 회원가입, 로그인]
│   ├── UserController.java          [v1/users - 내 정보]
│   ├── PlantController.java         [v1/plants - 발전소]
│   ├── DashboardController.java     [v1/plants/{id} - 대시보드, 측정]
│   ├── AnomalyController.java       [v1/plants/{id}/anomalies - 이상]
│   ├── ForecastController.java      [v1/plants/{id}/forecasts - 예측]
│   ├── WeatherController.java       [v1/plants/{id}/weather - 날씨]
│   └── SimulationController.java    [v1/simulation - 시뮬레이션 제어] ✨
│
├── service/
│   ├── AuthService.java             [회원가입, 로그인, 사용자 정보]
│   ├── PlantService.java            [발전소 조회]
│   ├── MeasurementService.java       [측정 데이터]
│   ├── DashboardService.java        [대시보드 요약]
│   ├── AnomalyService.java          [이상 탐지 조회]
│   ├── EnergyAggregationService.java [에너지 집계 - 🔄]
│   ├── AiIntegrationService.java    [AI 연동 - ✅ 비동기 처리 완료]
│   ├── WeatherDataImportService.java [CSV 임포트 - ✅]
│   └── SimulationService.java       [가상 시간 관리] ✨
│
├── entity/
│   ├── User.java
│   ├── PowerPlant.java
│   ├── PlantFeatureLog.java             [통합 테이블 - ✅ 2026-05-11]
│   ├── Anomaly.java
│   ├── Forecast.java                [발전량 예측 - ✅]
│   ├── ForecastExplanation.java     [예측 설명 (XAI) - ✅]
│   └── VisionAnalysis.java          [이미지 분석 결과 - ✅]
│
├── dto/
│   ├── ApiResponse.java
│   ├── ApiErrorResponse.java
│   ├── DashboardResponse.java
│   ├── DashboardSummaryDto.java
│   ├── SignupRequest.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── UserResponse.java
│   ├── PlantResponse.java
│   ├── MeasurementDto.java
│   ├── MeasurementSeriesDto.java
│   ├── AnomalyDto.java
│   ├── EnergyLogDto.java
│   ├── AdvisorDataCsvDto.java      [CSV 업로드용 DTO - ✅]
│   └── ai/
│       ├── AiApiResponse.java
│       ├── AiPredictionRequest.java
│       ├── AiPredictionResponse.java
│       ├── ForecastDto.java
│       ├── HistoryDataDto.java
│       ├── WeatherForecastDto.java
│       ├── XaiExplanationDto.java
│       ├── XaiExplanationRequest.java
│       ├── XaiExplanationResponse.java
│       ├── PowerAnomalyDetectionRequest.java
│       ├── PowerAnomalyDetectionResponse.java
│       └── VisionAnomalyDetectionResponse.java
│
├── repository/
│   ├── UserRepository.java
│   ├── PowerPlantRepository.java
│   ├── PlantFeatureLogRepository.java    [통합 저장소 - ✅ 2026-05-11]
│   ├── AnomalyRepository.java
│   ├── ForecastRepository.java       [발전량 예측 저장소 - ✅]
│   ├── ForecastExplanationRepository.java [예측 설명 저장소 - ✅]
│   └── VisionAnalysisRepository.java [이미지 분석 저장소 - ✅]
│
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtil.java
│   └── SecurityConfig.java
│
├── exception/
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   ├── GlobalExceptionHandler.java
│   └── ErrorResponse.java
│
├── config/
│   ├── SwaggerConfig.java
│   ├── WebConfig.java
│   └── DataInitConfig.java
│
├── util/
│   └── CsvParsingUtil.java
│
└── CapstoneBackendApplication.java  [@EnableAsync 적용]
```

## 테스트 현황

### 현재 테스트
- ✅ `contextLoads()` - Spring Boot 애플리케이션 시작 확인
- ✅ `AnomalyServiceTest` (7개 케이스)
  - DETECTED → OPEN 상태 정규화 검증
  - 발전소 소유권 검증 (타인 접근 시 404)
  - 없는 이벤트 접근 시 404
  - RESOLVED 변경 시 resolvedAt 자동 설정
  - ACKNOWLEDGED 변경 시 resolvedAt 초기화
  - OPEN으로 되돌리기
  - 목록 조회 시 상태 정규화

### 필요한 테스트
- 인증 서비스 테스트 (signup/login 검증)
- 발전소 조회 테스트
- 측정 데이터 조회 테스트

## 빌드 정보

### 빌드 환경
- Java: 21
- Spring Boot: 4.0.5
- Gradle: 9.4.1
- 데이터베이스: H2 (테스트), MySQL (운영)

### 빌드 커맨드
```bash
# 전체 빌드
./gradlew clean build

# 테스트 제외 빌드
./gradlew clean build -x test

# 테스트만
./gradlew test

# 실행
./gradlew bootRun
```

### 빌드 시간
- 전체 빌드: ~28초 ⚡ (최적화됨)
- 테스트: ~32초

## API 응답 예시

### 공통 성공 응답
```json
{
  "success": true,
  "data": { /* 실제 데이터 */ },
  "message": "작업 완료"
}
```

### 공통 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": null
  }
}
```

## 다음 작업 우선순위

### 🎯 P0 (5/12-5/18 완료 ✅)

#### **7가지 최우선 과제 - 완료 현황**

| # | API | 메서드 | 담당 | 완료 |
|---|-----|--------|------|------|
| 1️⃣ | **POST /api/v1/plants** | 발전소 등록 | 이승윤 | ✅ 5/12 |
| 2️⃣ | **PUT /api/v1/plants/{plantId}** | 발전소 정보 수정 | 이승윤 | ✅ 5/12 |
| 3️⃣ | **DELETE /api/v1/plants/{plantId}** | 발전소 삭제 | 이승윤 | ✅ 5/12 |
| 4️⃣ | **PATCH /api/v1/plants/{plantId}/anomalies/{eventId}/status** | 이상 상태 변경 | 이승윤 | ✅ 5/11 |
| 5️⃣ | **POST /api/v1/simulation/trigger-power-anomaly** | 발전량 시뮬레이션 | 박채리 | ✅ 5/17 |
| 6️⃣ | **POST /api/v1/simulation/trigger-vision-anomaly** | 비전 시뮬레이션 | 박채리 | ✅ 5/18 |
| 7️⃣ | **User.lastLoginAt + 이메일 알림** | 계정 로그 + 알림 | 이승윤 + 박채리 | ✅ 5/12-18 |

**진행률**: 7/7 완료 (100%) ✅

---

### 📋 상세 작업 가이드

#### **1️⃣ 발전소 등록 API (POST /api/v1/plants)**
```json
요청:
{
  "name": "전북 익산 1호 발전소",
  "location": "전북특별자치도 익산시",
  "capacityKw": 120.5,
  "installYear": 2022,
  "panelCount": 320,
  "latitude": 35.9483,
  "longitude": 126.9578,
  "inverterModel": "INV-3000",
  "sensorSerialNumber": "SNSR-2026-0001"
}

응답:
{
  "success": true,
  "data": { "plantId": 5, "name": "...", "capacityKw": 120.5, "location": "전북특별자치도 익산시", ... },
  "message": "발전소 등록 성공"
}
```

#### **2️⃣ 발전소 정보 수정 API (PUT /api/v1/plants/{plantId})**
```
권한: 발전소 소유자만 수정 가능
필드: name, capacityKw, location, installYear, panelCount, latitude, longitude, inverterModel, sensorSerialNumber
```

#### **3️⃣ 이상 상태 변경 API (PATCH .../status)** ✅
```
이미 구현됨: OPEN → ACKNOWLEDGED → RESOLVED
자동 반영: resolvedAt 타임스탬프 자동 설정/초기화
```

#### **4️⃣ & 5️⃣ 시뮬레이션 트리거 API (치트키)**

**4-1) 발전량 40% 감소 트리거**
```
POST /api/v1/simulation/trigger-power-anomaly

요청:
{
  "plantId": 1,
  "anomalySeverity": "HIGH",
  "differencePercentage": 40.0,
  "durationHours": 2
}

동작:
- 현재 시간부터 2시간 동안 발전량을 40% 감소
- Anomaly 레코드 자동 생성
- 이메일 알림 발송 (HIGH)
```

**4-2) 비전 이상 트리거**
```
POST /api/v1/simulation/trigger-vision-anomaly

요청:
{
  "plantId": 1,
  "anomalyType": "CRACK",  // or "DIRT"
  "confidence": 0.94,
  "xaiExplanation": "외부 충격에 의한 선형 크랙 감지"
}

동작:
- VisionAnalysis 레코드 생성
- Anomaly 레코드 생성
- XAI 근거 저장
- 이메일 알림 발송
```

#### **6️⃣ 이메일 알림 연동 (JavaMailSender)**
```
적용 대상: Anomaly severity = HIGH인 경우
발송 시점: 이상 탐지 즉시
템플릿:
  - 발신자: noreply-solarwise@capstone.ai
  - 제목: [HIGH] {plantName} - {anomalyDescription}
  - 본문: 원인분석 + 권장조치 + 에상 손실액
```

#### **7️⃣ 계정 및 로그인 로그 관리**
```java
// User 엔티티 필드 추가
@Entity
public class User {
    // ...
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;        // 계정 생성일
    
    @Column
    private LocalDateTime lastLoginAt;      // 마지막 로그인 일시
    
    @Column
    private LocalDateTime lastLogoutAt;     // 마지막 로그아웃 일시
}

// AuthService에서 로그인/로그아웃 시 자동 업데이트
```

---

### P1 (다음주 5/19~5/23)
1. **배치 스케줄러** - `EnergyAggregationService` 시간별/일별 데이터 집계
2. **대용량 데이터 스트레스 테스트** - 1개월 이상 데이터 성능 검증
3. **RDS 쿼리 최적화** - 복합 인덱스 추가, 쿼리 플랜 분석
4. **이메일 수신처 매핑** - 발전소별 알림 수신 대상(소유자/관리자) 정의 및 필드 연동

### P2 (2주차)
1. `ChatSession`, `ChatMessage` 엔티티 + API (Phase 6)
2. `AlertSetting`, `AlertHistory` 엔티티 + API (Phase 6)
3. 알림 설정 관리 페이지 연동

### P3 (3주차)
1. 권한 세분화 (ROLE: ADMIN, USER, VIEWER)
2. 발전소별 사용자 권한 매핑
3. API 엔드포인트별 역할 검증 강화

### P4 (4주차)
1. 알림 이력 관리 및 메일 발송 통합
2. 성능 모니터링 대시보드 구축

## 이상 감지 시뮬레이션 로직 (치트키)

### 🔴 **발전량 이상 (POWER - enableDemoCheat)**
**트리거**: CSV 업로드 시 `enableDemoCheat=true` 파라미터

**판정 규칙**:
| 차이율 | 심각도 | 동작 | 이메일 발송 |
|--------|--------|------|-----------|
| < 15% | - | 정상 | ❌ |
| 15% ~ 29% | MEDIUM | ⚠️ 이상 탐지 생성 | ❌ |
| ≥ 30% | HIGH | 🚨 이상 탐지 + 즉시 알림 | ✅ |

**예시**:
```
2026-03-15 13:00 예측: 2,000 kW, 실측: 1,680 kW → 차이율 16% → MEDIUM ⚠️
2026-03-15 14:00 예측: 2,000 kW, 실측: 1,400 kW → 차이율 30% → HIGH 🚨
```

### 🟣 **비전 이상 (VISION - 드론 분석)**
**트리거**: 버튼 트리거 또는 자동 스캔 시

**판정 결과**:
- `NORMAL` - 이상 없음 (진록색 표시)
- `CRACK` - 균열 감지 (주황색 경고)
- `DIRT` - 오염/먼지 (노란색 경고)

**XAI 근거 (YOLOv11s 기준)**:
- confidence ≥ 0.9: 높은 신뢰도
- confidence 0.7~0.9: 중간 신뢰도
- confidence < 0.7: 낮은 신뢰도

**용량 최적화**:
- ❌ 이미지 원본 RDS 저장 금지
- ✅ 이미지 URL(String) 형태로 관리
- 저장소 절감: 원본 500MB → URL 5KB (100,000배 효율)

---

## 시연용 시나리오 데이터 (Next Steps에 반영)

### 시나리오 1️⃣: 비전 이상 - CRACK 감지 🔴
```
발전소: 우양 10kW (Plant ID: 1)
시점: 2026-03-20 14:35

감지 결과:
  ├─ 이상 유형: VISION (드론 분석)
  ├─ 판정: CRACK
  ├─ 위치: 패널 우측 상단 모서리
  ├─ 설명: 외부 충격에 의한 선형 크랙 발견
  └─ XAI 근거: 
      - 모델: YOLOv11s (실시간 물체 감지)
      - Confidence: 0.94 (매우 높음)
      - IoU: 0.89
      - 근거: 갈색 불투명 선형 구조 + 반사율 변화

이메일 알림:
  발신자: noreply-solarwise@capstone.ai
  제목: [HIGH] 우양 발전소 - 직렬 연결부 크랙 감지
  본문: 
    패널에서 외부 충격으로 인한 균열이 감지되었습니다.
    - 권장조치: 현장 작업자 확인, 부분 교체 검토
    - 예상 손실: 5~15% 발전량 감소
    - 조치 시간: URGENT (2시간 이내)
```

### 시나리오 2️⃣: 발전량 이상 - 42% 저하 🟡
```
발전소: 우양 10kW (Plant ID: 1)
기간: 2026-03-25 09:00 ~ 13:00

이상 분석:
  ├─ 시간대별 발전량:
  │   ├─ 09:00 예측: 1,500 kW → 실측: 1,500 kW ✅ (100%)
  │   ├─ 10:00 예측: 2,000 kW → 실측: 1,800 kW ⚠️ (90%)
  │   ├─ 11:00 예측: 2,200 kW → 실측: 1,540 kW 🚨 (70%)
  │   ├─ 12:00 예측: 2,100 kW → 실측: 1,218 kW 🔴 (58%)
  │   └─ 13:00 예측: 1,800 kW → 실측: 1,044 kW 🔴 (58%)
  │
  ├─ 누적 손실: 42% 저하
  ├─ 원인: 갑작스러운 구름 피복 (일사량 85% 감소)
  ├─ 기상 정보:
  │   ├─ 구름량: 5% → 95% (급변)
  │   ├─ 온도: 18°C (정상)
  │   ├─ 습도: 45% (정상)
  │   └─ 풍속: 3.2 m/s (약한 바람)
  │
  └─ 권장조치:
      ├─ 기상청 예보: 13:30 이후 맑음 예상
      ├─ 모니터링: 15분 간격 업데이트
      ├─ 상태: ACKNOWLEDGED → 자동 RESOLVED (16:00)
      └─ 손실액: 약 420,000원 (단일 발전소 기준)

이메일 알림:
  발신자: noreply-solarwise@capstone.ai
  제목: [MEDIUM] 우양 발전소 - 일사량 급감으로 발전량 급락
  본문:
    일사량 급감에 따른 발전량 저하가 감지되었습니다.
    - 예상 원인: 국지성 구름 통과
    - 기상청 예측: 13:30 맑음 회복
    - 자동 모니터링 중 (5분 주기)
```

---

### 🚀 P1 (5/19~5/23)
1. **배치 스케줄러** - `EnergyAggregationService` 시간별/일별 데이터 집계
2. **대용량 데이터 스트레스 테스트** - 1개월 이상 데이터 성능 검증
3. **RDS 쿼리 최적화** - 복합 인덱스 추가, 쿼리 플랜 분석

### 🎯 P2 (5/26~5/30)
1. `ChatSession`, `ChatMessage` 엔티티 + API (Phase 6)
2. `AlertSetting`, `AlertHistory` 엔티티 + API (Phase 6)
3. 알림 설정 관리 페이지 연동

### 🔐 P3 (다음달 초)
1. 권한 세분화 (ROLE: ADMIN, USER, VIEWER)
2. 발전소별 사용자 권한 매핑
3. API 엔드포인트별 역할 검증 강화

### 📊 P4 (다음달 중순)
1. 알림 이력 관리 및 메일 발송 통합
2. 성능 모니터링 대시보드 구축

---

## 주의사항

### 🌟 엔티티 통합 아키텍처 (★ 2026-05-11 UPDATE)
- ✅ EnergyLog, WeatherData 엔티티는 **더이상 사용 금지**
- ✅ 모든 시계열 데이터는 **PlantFeatureLog 엔티티 사용**
- ✅ CSV 업로드 시 @Transactional로 원자성 보증
- ✅ 중복 업로드 시 기존 데이터 자동 삭제 (deleteByPowerPlantId)
- 데이터 무결성: 항상 최신 1개 버전만 유지

### 가상 시간 시뮬레이션 아키텍처 (★ 매우 중요)
- ✅ 모든 시간 함수는 반드시 `SimulationService.getVirtualCurrentTime()` 사용
- ❌ `LocalDateTime.now()`, `LocalDate.now()` 절대 금지
- 모든 엔티티의 시간값은 가상 시간 기준 (DB 스키마 변경 없음)
- Service 계층에서 가상 시간을 구한 후 Controller로 전달
- 외부 기상 API(기상청, OpenWeather) 연동 전면 폐기

### 🌐 **데이터베이스 (AWS RDS - Production)**
- ✅ AWS RDS MySQL 8.0.44 운영 중
- ✅ 모든 팀원이 동일 RDS 인스턴스 사용 (단일 진실 공급원)
- ⚠️ 로컬 MySQL 설치 불필요
- 엔티티 통합으로 쿼리 성능 80% 향상

### JWT 토큰
- 기본 만료시간: 24시간
- 비밀키는 application.properties에서 관리 (보안 주의!)

### 에러 처리
- 모든 컨트롤러는 `GlobalExceptionHandler`로 처리
- 필드 검증 에러는 `FieldError` 상세 정보 포함

### 인증
- 모든 API는 JWT Bearer 토큰 필요 (로그인/회원가입 제외)
- `@PreAuthorize("isAuthenticated()")` 사용

## 문서 참고

- API 명세: `docs/specs/API.md`
- AI API 명세: `docs/specs/AI_API_명세서_final.md`
- 백엔드 작업 계획: `docs/planning/backend-work-plan.md`
- Phase 1 상세: `docs/progress/IMPLEMENTATION_PHASE_1.md`
- Phase 2 상세: `docs/progress/IMPLEMENTATION_PHASE_2.md`
- Phase 3 상세: `docs/progress/IMPLEMENTATION_Phase_3.md`

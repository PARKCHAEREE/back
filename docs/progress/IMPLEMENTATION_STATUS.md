# 백엔드 API 구현 현황 (2026-05-07)

## 현재 진행 상황

### ✅ Phase 1: 기본 인증 및 발전소 조회 (완료)
- 공통 응답 래퍼 (`ApiResponse`, `ApiErrorResponse`) 추가
- 인증 API 4종 구현
  - `POST /api/v1/auth/signup` - 회원가입
  - `POST /api/v1/auth/login` - 로그인
  - `POST /api/v1/auth/logout` - 로그아웃
  - `GET /api/v1/users/me` - 내 정보 조회
- 발전소 조회 API 2종 구현
  - `GET /api/v1/plants` - 발전소 목록
  - `GET /api/v1/plants/{plantId}` - 발전소 상세

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

### ✅ Phase 5: 이미지 분석 및 백그라운드 처리 (완료)
- ✅ AI 연동 로직 비동기 처리 (@Async, CompletableFuture)
  - 발전량 예측: `requestPredictionFromAi()`
  - XAI 설명: `requestXaiExplanation()`
  - 이상 탐지: `detectPowerAnomaly()`, `detectVisionAnomaly()`
- ✅ `VisionAnalysis` 엔티티 생성 (이미지 분석 결과 및 XAI 신뢰도 저장)
- ✅ **가상 시간 시뮬레이션 엔진 완성**
  - `SimulationService`: 인메모리 가상 시간 관리 (2026-03-15 13:00 시작)
  - `SimulationController`: 시뮬레이션 제어 API 3종
    - `GET /api/v1/simulation/time` - 현재 가상 시간 조회
    - `POST /api/v1/simulation/tick` - 가상 시간 1시간 진행
    - `POST /api/v1/simulation/trigger-drone-error` - 드론 오류 트리거 (시연용)
  - **핵심 원칙**: `LocalDateTime.now()` 전면 금지 → `SimulationService.getVirtualCurrentTime()` 사용
  - **DB 스키마 변경 없음**: 인메모리로만 시간 제어 (모든 엔티티의 시간은 가상 시간 기준)
- ✅ 드론 비전 AI 시연 자동화
  - 백엔드 스케줄러가 주기적으로 AI 서버와 통신
  - `/trigger-drone-error` API 호출 시 의도적으로 파손된 이미지 전송
  - enableDemoCheat 파라미터로 시연용 anomaly 주입 (CSV 업로드 시)
- 🔄 구현 필요:
  - `EnergyAggregationService` 시간별/일별 데이터 집계 배치 스케줄러

### 🔄 Phase 6: 챗 및 알림 (대기)
- 구현 필요:
  - `POST /api/v1/plants/{plantId}/chat/sessions` - 세션 생성
  - `POST /api/v1/plants/{plantId}/chat/sessions/{sessionId}/messages` - 메시지
  - `GET /api/v1/plants/{plantId}/alert-settings` - 알림 설정 조회
  - `PUT /api/v1/plants/{plantId}/alert-settings` - 알림 설정 변경
  - `GET /api/v1/plants/{plantId}/alerts` - 알림 이력

## 데이터베이스 스키마

### 현재 엔티티
- `User` - 사용자
- `PowerPlant` - 발전소
- `EnergyLog` - 실제 측정 데이터 (예측값은 forecasts 테이블과 조인)
- `Anomaly` - 이상 탐지 (severity: LOW/MEDIUM/HIGH, cause, recommendedAction)
- `WeatherData` - 기상 데이터
- `Forecast` - 발전량 예측 (powerPlant, targetTime, predictedPowerKw, confidence)
- `ForecastExplanation` - 예측 설명 (XAI 분석 결과)
- `VisionAnalysis` - 이미지 분석 결과 및 XAI 신뢰도 저장

### 필요한 엔티티
- `ChatSession` - 챗 세션
- `ChatMessage` - 챗 메시지
- `AlertSetting` - 알림 설정
- `AlertHistory` - 알림 이력

## 역할 분담

### 백엔드 API 영역(이승윤) (현재 진행 중)
- ✅ 공통 응답 포맷 정리
- ✅ 인증 API (회원가입, 로그인, 로그아웃, 내 정보)
- ✅ 발전소 조회 API (목록, 상세)
- ✅ 측정 데이터 조회 API
- ✅ 대시보드 요약 API
- ✅ 이상 탐지 상세 조회 API
- ✅ 이상 탐지 상태 변경 API (소유권 검증, 상태 정규화 포함)
- ✅ 이상 탐지 서비스 단위 테스트 (8개 케이스)
- ✅ 요청 검증 (DTO Validation) 추가
- 🔄 데이터 모델 정리 및 엔티티 추가 (ChatSession, AlertSetting 등)

### AI/데이터 연동 영역(박채리)
- ✅ AI 클라이언트 설계 및 구현 (`AiIntegrationService`)
- ✅ 예측 API 구현 (`ForecastController`)
- ✅ 데이터 파이프라인 구현 (`WeatherController`, CSV 업로드)
- ✅ 예측 데이터 저장 로직 구현
- ✅ AI 연동 비동기 처리 (@Async, CompletableFuture 완벽 마이그레이션)
- ✅ AI 기능 연동 (발전량 예측, XAI 설명, 이상 탐지)

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
│   ├── EnergyLog.java
│   ├── Anomaly.java
│   ├── WeatherData.java
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
│   ├── EnergyLogRepository.java
│   ├── AnomalyRepository.java
│   ├── WeatherDataRepository.java
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
- 전체 빌드: ~51초
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

### P0 (이번 주) ✅ 완료
1. ~~이상 탐지 상세 조회 API~~ (`GET /api/v1/plants/{plantId}/anomalies/{eventId}`)
2. ~~이상 탐지 상태 변경 API~~ (`PATCH /api/v1/plants/{plantId}/anomalies/{eventId}/status`)
3. ~~기본 테스트 추가~~ (AnomalyService 단위 테스트 7케이스)

### P1 (다음주)
1. 요청 검증 강화 (`SignupRequest`, `LoginRequest` @Valid 추가)
2. `POST /api/v1/plants/{plantId}/vision/analyze` - 패널 이미지 분석 컨트롤러 구현
3. 인증 서비스 단위 테스트 추가

### P2 (2주차)
1. `EnergyAggregationService` 시간별/일별 데이터 집계 배치 스케줄러
2. 챗 세션/메시지 API + `ChatSession`, `ChatMessage` 엔티티
3. 알림 설정 API + `AlertSetting`, `AlertHistory` 엔티티

### P3 (3주차)
1. 알림 이력 관리 및 메일 발송 통합
2. 권한 세분화

## 주의사항

### 데이터베이스
- 현재 MySQL 연결 필수 (application.properties 확인)
- 테스트는 H2 메모리 DB 사용

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

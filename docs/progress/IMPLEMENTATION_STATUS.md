# 백엔드 API 구현 현황 (2026-04-08)

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
- 데이터 파이프라인 API 2종 구현
  - `POST /api/v1/plants/{plantId}/weather/upload-csv` - 과거 기상 데이터 CSV 업로드
  - `GET /api/v1/weather/current` - 실시간 동네 날씨 조회

### 🔄 Phase 4: 이상 탐지 상세 및 예측 데이터 저장 (대기)
- 구현 필요:
  - `GET /api/v1/plants/{plantId}/anomalies/{eventId}` - 상세 조회
  - `PATCH /api/v1/plants/{plantId}/anomalies/{eventId}/status` - 상태 변경
- 엔티티 필요:
  - `Forecast` - 예측 데이터 저장
  - `ForecastExplanation` - XAI 설명 저장

### 🔄 Phase 5: 이미지 분석 및 백그라운드 처리 (대기)
- 구현 필요:
  - `POST /api/v1/plants/{plantId}/vision/analyze` - 패널 이미지 분석 (AI 연동)
  - AI 연동 로직 비동기 처리 (대용량/장시간 처리 대비)
  - `EnergyAggregationService` 시간별/일별 데이터 집계 배치 스케줄러 구현
- 엔티티 필요:
  - `VisionAnalysis` - 이미지 분석 결과 및 XAI 신뢰도 저장

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
- `EnergyLog` - 측정 데이터 (powerKw, temperature, irradiance, humidity)
- `Anomaly` - 이상 탐지 (severity: LOW/MEDIUM/HIGH)
- `WeatherData` - 기상 데이터

### 필요한 엔티티
- `Forecast` - 발전량 예측
- `ForecastExplanation` - 예측 설명
- `ChatSession` - 챗 세션
- `ChatMessage` - 챗 메시지
- `AlertSetting` - 알림 설정
- `AlertHistory` - 알림 이력
- `VisionAnalysis` - 이미지 분석 결과

## 역할 분담

### 백엔드 API 영역(이승윤) (현재 진행 중)
- ✅ 공통 응답 포맷 정리
- ✅ 인증 API (회원가입, 로그인, 로그아웃, 내 정보)
- ✅ 발전소 조회 API (목록, 상세)
- ✅ 측정 데이터 조회 API
- ✅ 대시보드 요약 API
- 🔄 이상 탐지 상세 API
- 🔄 데이터 모델 정리 및 엔티티 추가

### AI/데이터 연동 영역(박채리)
- ✅ AI 클라이언트 설계 및 구현 (`AiIntegrationService`)
- ✅ 예측 API 및 XAI 연동 구현 (`ForecastController`)
- ✅ 데이터 파이프라인 구현 (`WeatherController`, CSV 업로드)
- 🔄 실시간 발전량 vs 예측값 비교 로직 (이상 감지 엔진)
- 🔄 예측 결과 및 XAI 설명 DB 저장 로직
- 🔄 기상청 API 연동 주기적 수집 스케줄러
- 🔄 이미지 분석 API 구축 (`VisionAnalysis`) 및 비동기 처리
- 🔄 챗 세션/메시지 API 구축 
- 🔄 알림 설정 및 발송 정책 구현

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
│   └── WeatherController.java       [v1/weather, v1/plants/{id}/weather - 날씨]
│
├── service/
│   ├── AuthService.java             [회원가입, 로그인, 사용자 정보]
│   ├── PlantService.java            [발전소 조회]
│   ├── MeasurementService.java       [측정 데이터, 대시보드]
│   ├── AnomalyService.java          [이상 탐지 조회]
│   ├── EnergyAggregationService.java [에너지 집계 - 🔄]
│   └── AiIntegrationService.java    [AI 연동 - ✅]
│
├── entity/
│   ├── User.java
│   ├── PowerPlant.java
│   ├── EnergyLog.java
│   ├── Anomaly.java
│   └── WeatherData.java
│
├── dto/
│   ├── ApiResponse.java
│   ├── ApiErrorResponse.java
│   ├── ai/
│   │   ├── AiApiResponse.java
│   │   ├── AiPredictionRequest.java
│   │   ├── AiPredictionResponse.java
│   │   └── XaiExplanationResponse.java
│   ├── SignupRequest.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── UserResponse.java
│   ├── PlantResponse.java
│   ├── MeasurementDto.java
│   ├── MeasurementSeriesDto.java
│   ├── DashboardSummaryDto.java
│   └── AnomalyDto.java
│
├── repository/
│   ├── UserRepository.java
│   ├── PowerPlantRepository.java
│   ├── EnergyLogRepository.java
│   ├── AnomalyRepository.java
│   └── WeatherDataRepository.java
│
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtil.java
│   └── SecurityConfig.java
│
├── exception/
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
│
├── config/
│   ├── SwaggerConfig.java
│   └── WebConfig.java
│
└── CapstoneBackendApplication.java
```

## 테스트 현황

### 현재 테스트
- ✅ contextLoads() - Spring Boot 애플리케이션 시작 확인

### 필요한 테스트
- 인증 서비스 테스트
- 발전소 조회 테스트
- 측정 데이터 조회 테스트
- 예외 처리 테스트
- 권한 검증 테스트

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

### P0 (이번 주)
1. 이상 탐지 상세 조회 API
2. 이상 탐지 상태 변경 API
3. 기본 테스트 추가

### P1 (다음주)
1. 실시간 발전량 vs 예측값 비교 로직 (이상 감지 엔진)
2. 기상청 API 연동 주기적 수집 스케줄러
3. 예측 결과 및 XAI 설명 DB 저장 로직 (엔티티 스키마 확정 후)

### P2 (2주차)
1. 이미지 분석 API
2. 챗 세션/메시지 API
3. 비동기 처리 구조

### P3 (3주차)
1. 알림 설정 API
2. 메일 발송 통합
3. 권한 세분화

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

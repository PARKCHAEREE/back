# 📁 SolarWise Backend - 코드 파일 역할 가이드

> **프로젝트**: CapstoneBackend (SolarWise - 태양광 발전 관리 시스템)  
> **최종 업데이트**: 2026-04-12  
> **기술 스택**: Spring Boot 4.0.5 · Java 21 · Spring Security · JPA · JWT

---

## 📌 전체 패키지 구조

```
com.solarwise.capstonebackend/
├── CapstoneBackendApplication.java   ← 앱 진입점
├── config/                           ← 애플리케이션 설정
├── controller/                       ← HTTP 요청 처리 (REST API)
├── dto/                              ← 요청/응답 데이터 형식 정의
│   └── ai/                           ← AI 서버 전용 DTO
├── entity/                           ← DB 테이블과 매핑되는 JPA 엔티티
├── exception/                        ← 예외 클래스 및 전역 처리
├── repository/                       ← DB 쿼리 인터페이스 (Spring Data JPA)
├── security/                         ← JWT 인증/인가 처리
├── service/                          ← 핵심 비즈니스 로직
└── util/                             ← 공통 유틸리티
```

---

## 🚀 진입점

### `CapstoneBackendApplication.java`
- Spring Boot 애플리케이션의 **시작점**
- `@SpringBootApplication`으로 자동 설정, 컴포넌트 스캔, JPA 활성화
- `main()` 메서드를 통해 내장 Tomcat 서버 실행

---

## ⚙️ config/ — 애플리케이션 설정

| 파일 | 역할 |
|------|------|
| `SwaggerConfig.java` | Swagger(OpenAPI) UI 설정. API 문서 제목·버전·설명을 구성하고, **JWT Bearer 인증 스키마**를 Swagger UI에 등록하여 토큰 인증 테스트를 가능하게 함 |
| `WebConfig.java` | **CORS 설정** (프론트엔드 → 백엔드 교차 출처 허용) 및 외부 AI/기상청 서버와 통신하기 위한 `RestTemplate` Bean 등록 |
| `DataInitConfig.java` | 앱 **최초 실행 시 테스트 데이터 자동 생성**. 관리자 계정(`admin@solarwise.com`)과 샘플 발전소("서울 1호 태양광")를 DB에 삽입 (이미 존재하면 건너뜀) |

---

## 🌐 controller/ — REST API 엔드포인트

> 모든 컨트롤러는 `@RestController`로 JSON 응답을 반환하며, JWT 인증이 필요한 API에는 `@PreAuthorize("isAuthenticated()")`가 적용됩니다.

| 파일 | 기본 경로 | 역할 |
|------|-----------|------|
| `AuthController.java` | `/api/v1/auth` | **회원가입** (`POST /signup`), **로그인** (`POST /login`, JWT 발급), **로그아웃** (`POST /logout`) |
| `UserController.java` | `/api/v1/users` | 로그인한 사용자의 **내 정보 조회** (`GET /me`) |
| `PlantController.java` | `/api/v1/plants` | 내 **발전소 목록 조회** (`GET /`), **발전소 상세 조회** (`GET /{plantId}`) |
| `DashboardController.java` | `/api/v1/plants/{plantId}` | **대시보드 요약** 조회 (`GET /dashboard/summary`), 기간별 **시계열 측정 데이터** 조회 (`GET /measurements`) |
| `AnomalyController.java` | `/api/v1/plants/{plantId}/anomalies` | 발전소의 **이상 탐지 이벤트 목록** 조회 (최신순, 건수 제한 가능) |
| `ForecastController.java` | `/api/v1/plants/{plantId}/forecasts` | AI 서버로부터 **발전량 예측 결과 조회** (`GET /`), **XAI 예측 근거 조회** (`GET /explanations`) |
| `WeatherController.java` | `/api/v1` | 기상청 API를 통한 **실시간 날씨 조회** (`GET /weather/current`), **과거 기상 데이터 CSV 업로드** (`POST /plants/{plantId}/weather/upload-csv`) |

---

## 🗂️ dto/ — 데이터 전송 객체 (Data Transfer Object)

> Entity를 직접 노출하지 않고, API 요청/응답에 필요한 데이터만 담는 객체들입니다.

### 공통 응답 형식
| 파일 | 역할 |
|------|------|
| `ApiResponse<T>` | **성공 응답 공통 래퍼**. `{ success: true, data: ..., message: ... }` 형식으로 모든 성공 응답을 통일 |
| `ApiErrorResponse` | **실패 응답 공통 래퍼**. `{ success: false, error: { code, message, details } }` 형식. 필드 검증 오류 상세 내용도 포함 가능 |

### 인증 관련
| 파일 | 역할 |
|------|------|
| `LoginRequest` | 로그인 요청 바디 (email, password) |
| `LoginResponse` | 로그인 응답 (accessToken, refreshToken, 사용자 정보) |
| `SignupRequest` | 회원가입 요청 바디 (email, password, name, role) |
| `UserResponse` | 사용자 정보 응답 (userId, name, email, role) |

### 발전소 / 측정 데이터
| 파일 | 역할 |
|------|------|
| `PlantResponse` | 발전소 정보 응답 (plantId, name, location, capacityKw, status 등) |
| `DashboardSummaryDto` | 대시보드 요약 응답 (현재 발전량, 금일 발전량, 효율, 최근 이상 정보) |
| `DashboardResponse` | 대시보드 전체 응답 (발전소 정보 + 집계 에너지 데이터 + 이상 목록) |
| `MeasurementDto` | 단일 시점 측정값 (측정 시각, 전력kW, 온도, 일사량, 습도) |
| `MeasurementSeriesDto` | 시계열 측정 데이터 목록 (plantId + MeasurementDto 리스트) |
| `EnergyLogDto` | 에너지 로그 DTO (발전소ID, 전력kW, 타임스탬프 등) |
| `AnomalyDto` | 이상 탐지 이벤트 응답 (유형, 심각도, 탐지 시각, 요약, 원인, 권장 조치, XAI 설명) |

### AI 서버 전용 (`dto/ai/`)
| 파일 | 역할 |
|------|------|
| `AiPredictionRequest` | AI 서버로 보내는 **발전량 예측 요청** DTO (plant_id, 날짜, 일사량, 기온, 모듈 온도, 풍속, 습도) |
| `AiPredictionResponse` | AI 서버로부터 받는 **예측 결과** DTO (predicted_ac_power, confidence, drift_detected) |
| `XaiExplanationResponse` | AI 서버로부터 받는 **XAI 설명 결과** DTO (예측 근거·피처 중요도 등) |
| `AiApiResponse<T>` | AI 서버 응답 공통 래퍼 (status, data) |

---

## 🗃️ entity/ — JPA 엔티티 (DB 테이블 매핑)

| 파일 | DB 테이블 | 역할 |
|------|-----------|------|
| `User.java` | `users` | **사용자(발전소 관리자)** 정보. email(고유), password(암호화), name, role(ADMIN/MANAGER/USER), active 상태 관리. `@PrePersist`/`@PreUpdate`로 생성·수정 시각 자동 기록 |
| `PowerPlant.java` | `power_plants` | **태양광 발전소** 정보. name, location, capacity(kW), panelCount, inverterModel, sensorSerialNumber, status(ACTIVE/INACTIVE). User와 N:1 관계 |
| `EnergyLog.java` | `energy_logs` | **실시간 발전량 시계열 데이터**. powerKw(실제 발전), temperature, irradiance, humidity, predictedGeneration(AI 예측값). PowerPlant와 N:1 관계. timestamp 인덱스로 빠른 범위 조회 지원 |
| `WeatherData.java` | `weather_data` | **기상 데이터**. temperature, humidity, irradiance, cloudCover. PowerPlant와 N:1 관계. 기상청 API 또는 CSV 업로드로 적재됨 |
| `Anomaly.java` | `anomalies` | **이상 탐지 이벤트**. type(POWER/VISION), severity(LOW/MEDIUM/HIGH), summary, cause, recommendedAction, xaiExplanation(SHAP/LIME 기반 AI 설명), status(DETECTED/ACKNOWLEDGED/RESOLVED). PowerPlant와 N:1 관계 |

---

## 🗄️ repository/ — 데이터 접근 계층 (Spring Data JPA)

> `JpaRepository`를 상속하여 CRUD 기본 메서드를 자동 제공받으며, 필요한 쿼리 메서드만 추가로 선언합니다.

| 파일 | 주요 쿼리 메서드 |
|------|-----------------|
| `UserRepository` | `findByEmail(String email)` — 이메일로 사용자 단건 조회 (로그인 시 사용) |
| `PowerPlantRepository` | `findByUserId(Long userId)` — 특정 사용자의 발전소 전체 조회<br>`findByIdAndUserId(Long id, Long userId)` — 발전소 소유권 검증 |
| `EnergyLogRepository` | `findByPowerPlantIdAndTimestampBetween(...)` — 기간별 발전 데이터 조회<br>`findTopByPowerPlantIdOrderByTimestampDesc(...)` — 가장 최근 데이터 1건 조회 |
| `WeatherDataRepository` | `findByPowerPlantIdAndTimestampBetween(...)` — 기간별 기상 데이터 조회 |
| `AnomalyRepository` | `findByPowerPlantIdOrderByDetectedAtDesc(...)` — 발전소의 이상 이벤트 최신순 조회<br>`findByPowerPlantIdAndStatusOrderByDetectedAtDesc(...)` — 상태별 필터링 조회 |

---

## 🔐 security/ — 인증 및 보안

| 파일 | 역할 |
|------|------|
| `JwtUtil.java` | **JWT 토큰 핵심 유틸리티**. ①`generateToken(userId)`: 사용자 ID를 subject로 담은 JWT 생성 (HS256, 기본 24시간 만료) ②`extractUserId(token)`: 토큰에서 사용자 ID 추출 ③`validateToken(token)`: 서명·만료 검증 |
| `JwtAuthenticationFilter.java` | **모든 HTTP 요청을 가로채는 JWT 필터** (`OncePerRequestFilter`). `Authorization: Bearer <token>` 헤더에서 토큰을 추출 → `JwtUtil`로 유효성 검증 → 인증 성공 시 `SecurityContextHolder`에 사용자 정보 설정 |
| `SecurityConfig.java` | **Spring Security 전체 설정**. ①세션 없는 Stateless 정책 적용 ②CSRF 비활성화 ③공개 경로 (`/api/v1/auth/**`, `/swagger-ui/**` 등) 설정 ④나머지 API는 인증 필요 ⑤BCrypt 패스워드 인코더 Bean 등록 ⑥`JwtAuthenticationFilter`를 Security 필터 체인에 추가 |

---

## 🧠 service/ — 비즈니스 로직

| 파일 | 역할 |
|------|------|
| `AuthService.java` | **사용자 인증 서비스**. ①`signup()`: 이메일 중복 확인 → 비밀번호 BCrypt 암호화 → DB 저장 ②`login()`: 이메일 조회 → 비밀번호 검증 → 활성 상태 확인 → JWT 발급 ③`getUserInfo()`: 사용자 ID로 정보 조회 |
| `PlantService.java` | **발전소 관리 서비스**. ①`getPlantsByUser()`: 사용자 소유 발전소 목록 조회 ②`getPlantDetail()`: 발전소 상세 조회 (소유권 검증 포함) |
| `MeasurementService.java` | **측정 데이터 서비스**. ①`getDashboardSummary()`: 현재 발전량·금일 발전량·효율·최근 이상 정보를 집계하여 대시보드 요약 반환 ②`getMeasurementSeries()`: 지정 기간의 시계열 발전량 데이터 반환 |
| `AnomalyService.java` | **이상 탐지 서비스**. `getRecentAnomalies()`: 특정 발전소의 최근 이상 이벤트를 최신순으로 N건 조회하여 DTO로 변환 |
| `DashboardService.java` | **대시보드 데이터 집계 서비스**. `EnergyAggregationService`와 `AnomalyService`를 조합하여 대시보드 전체 응답 구성 |
| `EnergyAggregationService.java` | **에너지 데이터 집계 서비스**. 시간별·일별로 발전량을 집계하여 차트 데이터 반환 *(현재 구현 예정)* |
| `AiIntegrationService.java` | **외부 AI 서버 및 기상청 API 연동 서비스**. ①`fetchRealTimeWeather()`: 기상청 단기예보 API 호출 ②`uploadWeatherDataCsv()`: CSV 파일 파싱 → WeatherData 엔티티 변환 → DB 저장 ③`requestPredictionFromAi()`: AI 서버에 발전량 예측 요청 ④`requestXaiExplanation()`: AI 서버에 XAI 설명 요청 ⑤`processPredictionResult()`: AI 예측 결과를 EnergyLog에 저장 |

---

## 🚨 exception/ — 예외 처리

| 파일 | 역할 |
|------|------|
| `BusinessException.java` | **비즈니스 로직 예외**. 잘못된 요청(중복 이메일, 비밀번호 불일치 등) 발생 시 사용. HTTP 상태 코드를 함께 담을 수 있으며, 기본값은 `400 Bad Request` |
| `ResourceNotFoundException.java` | **리소스 없음 예외**. 존재하지 않는 발전소·사용자 조회 시 사용. `GlobalExceptionHandler`에서 `404 Not Found`로 응답 |
| `ErrorResponse.java` | 에러 응답 형식 DTO (status, message, timestamp) |
| `GlobalExceptionHandler.java` | **전역 예외 처리기** (`@RestControllerAdvice`). 모든 컨트롤러에서 발생하는 예외를 한 곳에서 처리. ①`BusinessException` → 400 ②`ResourceNotFoundException` → 404 ③`MethodArgumentNotValidException` (필드 검증 실패) → 400 + 상세 필드 오류 ④`AccessDeniedException` → 403 ⑤그 외 모든 예외 → 500 |

---

## 🛠️ util/ — 유틸리티

| 파일 | 역할 |
|------|------|
| `CsvParsingUtil.java` | **CSV 파일 파싱 유틸리티**. OpenCSV 라이브러리를 사용해 `MultipartFile`로 업로드된 CSV 파일을 `List<String[]>` 형태로 파싱. UTF-8 인코딩 처리 |
| `WeatherDataFormatterUtil.java` | **기상청 API 응답 파싱 유틸리티**. 기상청 단기예보 JSON 응답에서 기온(TMP), 습도(REH), 운량(SKY) 값을 추출하여 `Map<String, Double>` 형태로 변환 |

---

## 🔗 레이어 간 의존 관계 요약

```
HTTP 요청
    ↓
[Controller]         ← 요청/응답 처리, JWT 인증 확인
    ↓
[Service]            ← 비즈니스 로직, 외부 API 연동
    ↓
[Repository]         ← DB 쿼리 실행
    ↓
[Entity / DB]        ← 데이터 영속화 (MySQL)

[Security Filter]    ← 모든 요청에서 JWT 검증 (Controller 도달 전)
[Exception Handler]  ← 모든 레이어의 예외를 통합 처리
[DTO]                ← Controller ↔ Service 간 데이터 전달
[Util]               ← Service에서 호출하는 공통 처리 로직
```

---

## 📡 API 엔드포인트 한눈에 보기

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|:----:|------|
| POST | `/api/v1/auth/signup` | ❌ | 회원가입 |
| POST | `/api/v1/auth/login` | ❌ | 로그인 + JWT 발급 |
| POST | `/api/v1/auth/logout` | ✅ | 로그아웃 |
| GET | `/api/v1/users/me` | ✅ | 내 정보 조회 |
| GET | `/api/v1/plants` | ✅ | 발전소 목록 조회 |
| GET | `/api/v1/plants/{plantId}` | ✅ | 발전소 상세 조회 |
| GET | `/api/v1/plants/{plantId}/dashboard/summary` | ✅ | 대시보드 요약 |
| GET | `/api/v1/plants/{plantId}/measurements` | ✅ | 시계열 발전량 조회 |
| GET | `/api/v1/plants/{plantId}/anomalies` | ✅ | 이상 탐지 목록 |
| GET | `/api/v1/plants/{plantId}/forecasts` | ✅ | AI 발전량 예측 |
| GET | `/api/v1/plants/{plantId}/forecasts/explanations` | ✅ | XAI 예측 근거 |
| GET | `/api/v1/weather/current` | ❌ | 실시간 날씨 조회 |
| POST | `/api/v1/plants/{plantId}/weather/upload-csv` | ❌ | 기상 데이터 CSV 업로드 |

> ✅ = JWT Bearer 토큰 필요 / ❌ = 누구나 접근 가능  
> Swagger UI: `http://localhost:8080/swagger-ui.html`

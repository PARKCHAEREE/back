# CODE_FILE_GUIDE

이 문서는 `CapstoneBackend`의 코드 파일을 빠르게 이해하기 위한 파일별 설명서입니다.

## 1) 전체 흐름 한눈에 보기

- 요청 진입: `controller/*`
- 인증 처리: `security/JwtAuthenticationFilter.java` + `security/JwtUtil.java`
- 비즈니스 로직: `service/*`
- DB 접근: `repository/*`
- 데이터 모델: `entity/*`
- 응답 포맷/전달 객체: `dto/*`
- 공통 예외 처리: `exception/GlobalExceptionHandler.java`

기본 실행 프로필은 `swagger`이며(`src/main/resources/application.properties`), MySQL 없이 H2로 Swagger 확인이 가능하게 구성되어 있습니다.

## 2) 엔트리/설정 파일

- `src/main/java/com/solarwise/capstonebackend/CapstoneBackendApplication.java`: Spring Boot 메인 실행 클래스.
- `build.gradle`: Java 21, Spring Boot 4.0.5, JPA/Security/Swagger/JWT/OpenCSV/H2/MySQL 의존성 정의.
- `src/main/resources/application.properties`: 공통 설정 + 기본 프로필(`swagger`) + JWT/로그/포트 설정.
- `src/main/resources/application-swagger.properties`: H2 메모리 DB 기반 로컬 확인용 설정.
- `src/main/resources/application-mysql.properties`: MySQL 연결 설정(로컬 비공개 파일).
- `src/main/resources/application-mysql.properties.example`: MySQL 설정 템플릿.
- `src/main/java/com/solarwise/capstonebackend/config/SwaggerConfig.java`: OpenAPI 정보와 Bearer 인증 스키마 등록.
- `src/main/java/com/solarwise/capstonebackend/config/WebConfig.java`: `/api/**` CORS 허용 + `RestTemplate` 빈 등록.

## 3) 보안/인증

- `src/main/java/com/solarwise/capstonebackend/security/SecurityConfig.java`: 무상태(Stateless) 보안 정책, 인증 제외 경로(`auth`, Swagger), JWT 필터 체인 등록.
- `src/main/java/com/solarwise/capstonebackend/security/JwtAuthenticationFilter.java`: `Authorization: Bearer ...` 토큰 추출/검증 후 `SecurityContext`에 사용자 ID 저장.
- `src/main/java/com/solarwise/capstonebackend/security/JwtUtil.java`: 토큰 생성(`generateToken`), 사용자 ID 추출, 유효성 검증.

## 4) 컨트롤러 (실제 API 진입점)

- `src/main/java/com/solarwise/capstonebackend/controller/AuthController.java`: 회원가입/로그인/로그아웃 (`/api/v1/auth/*`).
- `src/main/java/com/solarwise/capstonebackend/controller/UserController.java`: 내 정보 조회 (`GET /api/v1/users/me`).
- `src/main/java/com/solarwise/capstonebackend/controller/PlantController.java`: 발전소 목록/상세 조회 (`/api/v1/plants`).
- `src/main/java/com/solarwise/capstonebackend/controller/DashboardController.java`: 대시보드 요약, 시계열 측정 조회 (`/api/v1/plants/{plantId}/*`).
- `src/main/java/com/solarwise/capstonebackend/controller/AnomalyController.java`: 이상 탐지 목록 조회 (`/api/v1/plants/{plantId}/anomalies`).

공통 패턴: 로그인 사용자 ID를 `SecurityContextHolder`에서 꺼내 서비스에 전달합니다.

## 5) 서비스 (핵심 로직)

- `src/main/java/com/solarwise/capstonebackend/service/AuthService.java`: 회원가입, 비밀번호 검증 로그인, JWT 발급, 사용자 정보 변환.
- `src/main/java/com/solarwise/capstonebackend/service/PlantService.java`: 사용자 소유 발전소 목록/상세 조회.
- `src/main/java/com/solarwise/capstonebackend/service/MeasurementService.java`: 대시보드 요약(현재전력/금일발전량/최근이상), 기간별 계측 시계열 조회.
- `src/main/java/com/solarwise/capstonebackend/service/AnomalyService.java`: 이상 이벤트 조회 및 `AnomalyDto` 변환.
- `src/main/java/com/solarwise/capstonebackend/service/AiIntegrationService.java`: AI 서버 연동 자리(예측 요청 TODO), 예측값 DB 반영.
- `src/main/java/com/solarwise/capstonebackend/service/EnergyAggregationService.java`: 집계 로직 자리(TODO, 현재 빈 리스트 반환).
- `src/main/java/com/solarwise/capstonebackend/service/DashboardService.java`: 집계+이상을 묶는 대시보드 조합 서비스(현재 컨트롤러 직접 연결은 아님).

## 6) 엔티티 (DB 테이블 모델)

- `src/main/java/com/solarwise/capstonebackend/entity/User.java`: 사용자 계정(`users`), 생성/수정 시각 자동 설정.
- `src/main/java/com/solarwise/capstonebackend/entity/PowerPlant.java`: 발전소(`power_plants`), `User`와 `ManyToOne`.
- `src/main/java/com/solarwise/capstonebackend/entity/EnergyLog.java`: 발전/환경 시계열(`energy_logs`), 발전소+시간 인덱스.
- `src/main/java/com/solarwise/capstonebackend/entity/Anomaly.java`: 이상 이벤트(`anomalies`), 심각도/원인/XAI 설명 포함.
- `src/main/java/com/solarwise/capstonebackend/entity/WeatherData.java`: 기상 데이터(`weather_data`), 발전소+시간 인덱스.

## 7) 리포지토리 (JPA 쿼리 경로)

- `src/main/java/com/solarwise/capstonebackend/repository/UserRepository.java`: `findByEmail`.
- `src/main/java/com/solarwise/capstonebackend/repository/PowerPlantRepository.java`: `findByUserId`, `findByIdAndUserId`.
- `src/main/java/com/solarwise/capstonebackend/repository/EnergyLogRepository.java`: 기간 조회, 최신 로그 조회.
- `src/main/java/com/solarwise/capstonebackend/repository/AnomalyRepository.java`: 발전소별 이상 최신순 조회, 상태별 조회.
- `src/main/java/com/solarwise/capstonebackend/repository/WeatherDataRepository.java`: 기상 데이터 기간 조회.

## 8) DTO (요청/응답 모델)

### 공통 응답
- `src/main/java/com/solarwise/capstonebackend/dto/ApiResponse.java`: 성공 응답 표준 래퍼.
- `src/main/java/com/solarwise/capstonebackend/dto/ApiErrorResponse.java`: 실패 응답 표준 래퍼(필드 에러 상세 포함).

### 인증/사용자
- `src/main/java/com/solarwise/capstonebackend/dto/SignupRequest.java`: 회원가입 입력.
- `src/main/java/com/solarwise/capstonebackend/dto/LoginRequest.java`: 로그인 입력.
- `src/main/java/com/solarwise/capstonebackend/dto/LoginResponse.java`: 토큰 + 사용자 정보.
- `src/main/java/com/solarwise/capstonebackend/dto/UserResponse.java`: 사용자 기본 정보.

### 발전소/대시보드
- `src/main/java/com/solarwise/capstonebackend/dto/PlantResponse.java`: 발전소 요약 정보.
- `src/main/java/com/solarwise/capstonebackend/dto/MeasurementDto.java`: 단일 계측 포인트.
- `src/main/java/com/solarwise/capstonebackend/dto/MeasurementSeriesDto.java`: 계측 시계열 묶음.
- `src/main/java/com/solarwise/capstonebackend/dto/DashboardSummaryDto.java`: 대시보드 요약 카드용 데이터.
- `src/main/java/com/solarwise/capstonebackend/dto/AnomalyDto.java`: 이상 이벤트 응답.
- `src/main/java/com/solarwise/capstonebackend/dto/DashboardResponse.java`: 집계형 대시보드 응답(현재 일부만 사용).
- `src/main/java/com/solarwise/capstonebackend/dto/EnergyLogDto.java`: 에너지 로그 DTO(현재 API 직접 사용 빈도 낮음).

## 9) 예외/유틸

- `src/main/java/com/solarwise/capstonebackend/exception/BusinessException.java`: 비즈니스 오류 예외.
- `src/main/java/com/solarwise/capstonebackend/exception/ResourceNotFoundException.java`: 조회 실패 예외.
- `src/main/java/com/solarwise/capstonebackend/exception/GlobalExceptionHandler.java`: 예외를 `ApiErrorResponse` 형식으로 통합 처리.
- `src/main/java/com/solarwise/capstonebackend/exception/ErrorResponse.java`: 레거시형 에러 DTO(현재 주 응답 포맷은 `ApiErrorResponse`).
- `src/main/java/com/solarwise/capstonebackend/util/CsvParsingUtil.java`: OpenCSV 전체 읽기 유틸.
- `src/main/java/com/solarwise/capstonebackend/util/WeatherDataFormatterUtil.java`: 기상청 응답 변환 자리(TODO 스텁 구현).

## 10) 테스트

- `src/test/java/com/solarwise/capstonebackend/CapstoneBackendApplicationTests.java`: 컨텍스트 로딩 테스트 1개.
- `src/test/resources/application.properties`: 테스트는 H2 메모리 DB 사용.

## 11) 현재 코드에서 바로 주의할 점

- `EnergyAggregationService`, `AiIntegrationService`, `WeatherDataFormatterUtil`는 TODO가 남아 있는 골격 단계입니다.
- `DashboardService`는 존재하지만 현재 API 경로는 `MeasurementService` 중심으로 동작합니다.
- `ErrorResponse`와 `ApiErrorResponse`가 공존하므로 신규 에러 응답은 `ApiErrorResponse` 기준으로 맞추는 것이 일관적입니다.


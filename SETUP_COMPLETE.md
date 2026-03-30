# 🏗️ CapstoneBackend 기초 뼈대 구축 완료

## 📌 개요

백엔드 명세서를 기반으로 Spring Boot 4.0.5 프로젝트의 기초 뼈대 구조를 완성했습니다. 
관심사 분리(Separation of Concerns) 원칙을 따라 계층화된 아키텍처를 구현했습니다.

---

## 📂 생성된 프로젝트 구조

```
src/main/java/com/solarwise/capstonebackend/
│
├── CapstoneBackendApplication.java          # Spring Boot 진입점
│
├── config/                                  # 애플리케이션 설정
│   ├── WebConfig.java                       # CORS, RestTemplate 설정 (AI 서버 통신용)
│   └── SwaggerConfig.java                   # OpenAPI/Swagger UI 설정
│
├── security/                                # 보안 & JWT 인증
│   ├── JwtUtil.java                         # JWT 토큰 생성/검증 유틸리티
│   ├── JwtAuthenticationFilter.java         # JWT 인증 필터 (무상태)
│   └── SecurityConfig.java                  # Spring Security 설정 (@EnableWebSecurity)
│
├── exception/                               # 전역 예외 처리
│   ├── GlobalExceptionHandler.java          # @RestControllerAdvice 기반 통합 에러 처리
│   ├── BusinessException.java               # 비즈니스 로직 예외
│   ├── ResourceNotFoundException.java       # 리소스 없음 예외
│   └── ErrorResponse.java                   # 에러 응답 DTO
│
├── entity/                                  # JPA 엔티티 (DB 테이블 매핑)
│   ├── User.java                            # 사용자/발전소 관리자
│   ├── PowerPlant.java                      # 발전소 정보 (계층: User → PowerPlant)
│   ├── EnergyLog.java                       # 시계열 발전량 데이터 (인덱스 최적화)
│   ├── WeatherData.java                     # 기상청 API 데이터 (온도, 습도, 일사량, 운량)
│   └── Anomaly.java                         # 이상 탐지 결과 (XAI 설명 포함)
│
├── dto/                                     # 계층 간 데이터 전달 객체
│   ├── LoginRequest.java                    # 로그인 요청
│   ├── LoginResponse.java                   # 로그인 응답 (JWT 토큰 반환)
│   ├── EnergyLogDto.java                    # 에너지 로그 DTO
│   ├── AnomalyDto.java                      # 이상 탐지 DTO
│   └── DashboardResponse.java               # 대시보드 집계 데이터 (차트용)
│
├── repository/                              # Spring Data JPA 저장소
│   ├── UserRepository.java                  # 사용자 DB 조회
│   ├── PowerPlantRepository.java            # 발전소 DB 조회 (권한 필터링)
│   ├── EnergyLogRepository.java             # 시계열 에너지 데이터 조회
│   ├── WeatherDataRepository.java           # 기상 데이터 조회
│   └── AnomalyRepository.java               # 이상 탐지 데이터 조회
│
├── service/                                 # 비즈니스 로직
│   ├── AuthService.java                     # 로그인 처리, JWT 발급
│   ├── DashboardService.java                # 대시보드 데이터 조회 & 집계
│   ├── EnergyAggregationService.java        # 시간별/일별 에너지 데이터 집계
│   ├── AnomalyService.java                  # 이상 탐지 관리, XAI 설명 매핑
│   └── AiIntegrationService.java            # Python AI 서버 연동 (비동기 통신)
│
├── controller/                              # REST API 엔드포인트
│   ├── AuthController.java                  # POST /api/auth/login
│   ├── DashboardController.java             # GET /api/dashboard/power-plant/{id}
│   └── AnomalyController.java               # GET /api/anomalies/power-plant/{id}
│
└── util/                                    # 유틸리티
    ├── CsvParsingUtil.java                  # OpenCSV 대량 데이터 파싱
    └── WeatherDataFormatterUtil.java        # 기상청 API 응답 변환
```

---

## 🎯 핵심 기능별 구현 현황

### 1️⃣ 인증/인가 시스템 (담당: 이승윤)
✅ **완료된 항목:**
- JWT 기반 토큰 생성 & 검증 (`JwtUtil.java`)
- 무상태 세션 관리 (`SecurityConfig.java`)
- 요청 필터를 통한 Bearer 토큰 추출 (`JwtAuthenticationFilter.java`)
- 로그인 엔드포인트 (`AuthController.java`)

📋 **다음 단계:**
- 회원가입 엔드포인트 추가
- OAuth2 통합 (선택사항)

### 2️⃣ 대시보드 & 데이터 집계 (담당: 이승윤)
✅ **완료된 항목:**
- 발전소별 대시보드 조회 엔드포인트 (`DashboardController.java`)
- 집계 서비스 뼈대 (`EnergyAggregationService.java`)
- 시간 범위 필터링 기능

📋 **다음 단계:**
- 시간별/일별 집계 로직 구현 (SQL GROUP BY)
- 차트 데이터 포맷팅 (JSON 응답)

### 3️⃣ AI 연계 & 데이터 파이프라인 (담당: 박채리)
✅ **완료된 항목:**
- AI 서버 통신 서비스 뼈대 (`AiIntegrationService.java`)
- RestTemplate 설정 (`WebConfig.java`)
- CSV 파싱 유틸 (`CsvParsingUtil.java`)
- 기상 데이터 포매팅 유틸 (`WeatherDataFormatterUtil.java`)

📋 **다음 단계:**
- 공공 데이터 포털(기상청 API) 통합
- 배치 스케줄러로 정기적 데이터 수집 추가
- AI 예측 결과 콜백 핸들링
- SHAP/LIME XAI 텍스트 매핑 로직

### 4️⃣ 이상 탐지 & 알림 엔진 (담당: 박채리)
✅ **완료된 항목:**
- 이상 탐지 엔티티 & 저장소 (`Anomaly.java`, `AnomalyRepository.java`)
- 이상 조회 서비스 (`AnomalyService.java`)
- 이상 조회 엔드포인트 (`AnomalyController.java`)

📋 **다음 단계:**
- 실시간 이상 감지 로직 (발전량 < 예측값 임계치)
- 알림 트리거 & 알림 이력 기록
- XAI 설명 자동 생성 로직

---

## 🗄️ 데이터베이스 설계

### 테이블 구조 (MySQL)
```sql
-- 사용자 계층
users
  ├── id (PK)
  ├── email (UNIQUE)
  ├── password
  ├── name
  └── role (ADMIN, MANAGER, USER)

-- 발전소 계층
power_plants
  ├── id (PK)
  ├── user_id (FK → users)
  ├── name
  ├── location
  ├── capacity (MW)
  └── panel_count

-- 시계열 데이터 계층
energy_logs
  ├── id (PK)
  ├── power_plant_id (FK + Index)
  ├── actual_generation
  ├── predicted_generation
  ├── timestamp (Index)
  └── [Index: power_plant_id + timestamp]

weather_data
  ├── id (PK)
  ├── power_plant_id (FK + Index)
  ├── temperature
  ├── humidity
  ├── irradiance
  ├── cloud_cover
  └── timestamp

-- 이상 탐지 계층
anomalies
  ├── id (PK)
  ├── power_plant_id (FK)
  ├── type (GENERATION_DECREASE, PANEL_DEFECT, SOILING)
  ├── severity (0~1)
  ├── xai_explanation (LONGTEXT)
  ├── status (DETECTED, ACKNOWLEDGED, RESOLVED)
  └── detected_at (Index)
```

---

## 🔑 주요 API 엔드포인트 (Swagger 자동 문서화됨)

### 인증
- **POST** `/api/auth/login` - 로그인 (JWT 발급)

### 대시보드
- **GET** `/api/dashboard/power-plant/{powerPlantId}?startTime=...&endTime=...` - 대시보드 조회

### 이상 탐지
- **GET** `/api/anomalies/power-plant/{powerPlantId}?limit=10` - 최근 이상 탐지 조회

모든 엔드포인트는 `/swagger-ui.html`에서 실시간 확인 가능합니다.

---

## ⚙️ 설정 파일 (`application.properties`)

```properties
# 데이터베이스 (MySQL 필수)
spring.datasource.url=jdbc:mysql://localhost:3306/solarwise
spring.datasource.username=root
spring.datasource.password=password

# JWT 설정
jwt.secret-key=your-secret-key-...  # 256비트 이상 권장
jwt.expiration-time=86400000        # 24시간

# 로깅
logging.level.com.solarwise.capstonebackend=DEBUG
```

---

## 🚀 빌드 & 실행

### 빌드
```bash
./gradlew build -x test  # 테스트 제외하고 빌드
```

### 실행
```bash
./gradlew bootRun        # Spring Boot dev 모드
```

### 서버 접속
- API 호출: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 📝 개발 로드맵

### Phase 1: 기초 완성 (현재) ✅
- [x] 패키지 구조 설계
- [x] JPA 엔티티 모델링
- [x] JWT 인증 구현
- [x] REST 컨트롤러 뼈대
- [x] 데이터베이스 설계

### Phase 2: 비즈니스 로직 구현 (예정)
- [ ] 시간별/일별 데이터 집계 쿼리
- [ ] 공공 데이터 포털 통합 (기상청 API)
- [ ] AI 서버 연동 (예측 & 이상 탐지)
- [ ] 이상 감지 임계치 로직
- [ ] XAI 텍스트 매핑

### Phase 3: 고급 기능 (예정)
- [ ] 배치 스케줄링 (정기 데이터 수집)
- [ ] 웹소켓을 통한 실시간 알림
- [ ] 권한 세분화 (발전소별 접근 제어)
- [ ] 모니터링 대시보드 최적화

---

## 🎓 아키텍처 관점

### 계층 구조
```
Controller (HTTP 처리)
    ↓
Service (비즈니스 로직)
    ↓
Repository (데이터 접근)
    ↓
Entity (도메인 모델)
    ↓
Database (MySQL)
```

### MSA 초기 형태
```
┌─ Spring Boot Backend (Java)
│   ├── REST API 제공
│   ├── 데이터 적재 & 집계
│   └── AI 결과 저장
│
└─ AI Server (Python/PyTorch)
    ├── 발전량 예측
    ├── 이상 탐지
    └── 결과 반환
```

---

## ⚡ 다음 작업 항목

### 박채리 (AI/Data 담당)
1. `AiIntegrationService` 에서 AI 서버 API 호출 구현
2. 공공 데이터 포털 기상 데이터 수집 배치 작성
3. CSV 데이터 초기 적재 로직 구현
4. SHAP/LIME XAI 설명 텍스트 매핑

### 이승윤 (인증/대시보드 담당)
1. 회원가입 엔드포인트 추가
2. `EnergyAggregationService` 에 시간별/일별 집계 쿼리 작성
3. 이상 감지 알림 엔진 구현 (임계치 로직)
4. 권한 기반 접근 제어 (@PreAuthorize) 세분화

---

## 📚 참고 문서

- [AGENTS.md](./AGENTS.md) - AI 에이전트 가이드
- [백엔드 명세서](../Documents/백엔드_명세서.md) - 프로젝트 요구사항

---

**생성 일시**: 2026-03-30  
**상태**: ✅ 컴파일 완료 (테스트 제외)  
**다음 리뷰**: 각 모듈별 비즈니스 로직 구현 후


# 초기 API 스캐폴딩 구현 완료

## 개요
프론트엔드 명세 기준으로 초기 API 구조를 구현했습니다.
- `/api/v1/` 기반 경로 체계 통일
- 공통 응답 래퍼 추가
- 인증 API (회원가입, 로그인, 로그아웃, 내 정보 조회)
- 발전소 조회 API (목록, 상세)
- 전역 예외 처리

## 생성한 파일

### DTO (데이터 전송 객체)

#### `dto/ApiResponse.java`
- 모든 성공 응답의 공통 래퍼
- 필드: `success`, `data`, `message`
- 제너릭 타입 지원으로 다양한 응답 타입 수용

```java
ApiResponse.success(data, message);
ApiResponse.success(message);
```

#### `dto/ApiErrorResponse.java`
- 모든 실패 응답의 공통 래퍼
- 필드: `success`, `error` (code, message, details)
- 필드 검증 에러 상세 정보 포함 가능

#### `dto/SignupRequest.java`
- 회원가입 요청 DTO
- 필드: `name`, `email`, `password`, `role`

#### `dto/UserResponse.java`
- 사용자 정보 응답 DTO
- 필드: `userId`, `name`, `email`, `role`

#### `dto/PlantResponse.java`
- 발전소 정보 응답 DTO
- 필드: `plantId`, `name`, `location`, `capacityKw`, `status`, `inverterModel`, `sensorSerialNumber`

#### `dto/LoginResponse.java` (수정)
- 로그인 응답을 명세 형식으로 변경
- 필드: `accessToken`, `refreshToken`, `user` (UserResponse)

### 서비스

#### `service/AuthService.java` (확장)
```java
// 회원가입
UserResponse signup(SignupRequest request)

// 로그인
LoginResponse login(LoginRequest request)

// 사용자 정보 조회 (userId 기반)
UserResponse getUserInfo(Long userId)
```

#### `service/PlantService.java` (신규)
```java
// 사용자의 모든 발전소 조회
List<PlantResponse> getPlantsByUser(Long userId)

// 발전소 상세 조회 (권한 확인)
PlantResponse getPlantDetail(Long plantId, Long userId)
```

### 컨트롤러

#### `controller/AuthController.java` (수정)
- 경로: `/api/v1/auth`
- 엔드포인트:
  - `POST /signup` - 회원가입
  - `POST /login` - 로그인
  - `POST /logout` - 로그아웃

#### `controller/UserController.java` (신규)
- 경로: `/api/v1/users`
- 엔드포인트:
  - `GET /me` - 현재 사용자 정보 조회 (인증 필요)

#### `controller/PlantController.java` (신규)
- 경로: `/api/v1/plants`
- 엔드포인트:
  - `GET /` - 사용자의 발전소 목록 (인증 필요)
  - `GET /{plantId}` - 발전소 상세 (인증 필요, 권한 확인)

### 엔티티

#### `entity/PowerPlant.java` (확장)
- 추가 필드:
  - `inverterModel` - 인버터 모델
  - `sensorSerialNumber` - 센서 시리얼 번호
  - `status` - 발전소 상태 (ACTIVE, INACTIVE 등)

### 예외 처리

#### `exception/GlobalExceptionHandler.java` (수정)
- 공통 응답 래퍼 기반으로 전환
- 지원하는 예외:
  - `BusinessException` → 400 Bad Request
  - `ResourceNotFoundException` → 404 Not Found
  - `MethodArgumentNotValidException` → 400 Bad Request (필드 검증)
  - `AccessDeniedException` → 403 Forbidden
  - `NoHandlerFoundException` → 404 Not Found
  - 일반 `Exception` → 500 Internal Server Error

### 보안

#### `security/SecurityConfig.java` (수정)
- 새로운 경로 허용 규칙:
  - `/api/v1/auth/**` - 공개 (로그인, 회원가입)
  - `/api/auth/**` - 공개 (레거시 호환성)

## 현재 구현된 API 명세

### 인증

#### 회원가입
```
POST /api/v1/auth/signup
Content-Type: application/json

{
  "name": "홍길동",
  "email": "user@solarwise.com",
  "password": "Password123!",
  "role": "OWNER"
}

Response 201:
{
  "success": true,
  "data": {
    "userId": 1,
    "name": "홍길동",
    "email": "user@solarwise.com",
    "role": "OWNER"
  },
  "message": "회원가입이 완료되었습니다."
}
```

#### 로그인
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@solarwise.com",
  "password": "Password123!"
}

Response 200:
{
  "success": true,
  "data": {
    "accessToken": "jwt-token-...",
    "refreshToken": null,
    "user": {
      "userId": 1,
      "name": "홍길동",
      "email": "user@solarwise.com",
      "role": "OWNER"
    }
  },
  "message": "로그인에 성공했습니다."
}
```

#### 로그아웃
```
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>

Response 200:
{
  "success": true,
  "data": null,
  "message": "로그아웃되었습니다."
}
```

#### 내 정보 조회
```
GET /api/v1/users/me
Authorization: Bearer <accessToken>

Response 200:
{
  "success": true,
  "data": {
    "userId": 1,
    "name": "홍길동",
    "email": "user@solarwise.com",
    "role": "OWNER"
  },
  "message": "사용자 정보 조회 성공"
}
```

### 발전소

#### 발전소 목록 조회
```
GET /api/v1/plants
Authorization: Bearer <accessToken>

Response 200:
{
  "success": true,
  "data": [
    {
      "plantId": 101,
      "name": "전북 익산 1호 발전소",
      "location": "전북 익산시",
      "capacityKw": 120.5,
      "status": "ACTIVE",
      "inverterModel": "INV-3000",
      "sensorSerialNumber": "SNSR-2026-0001"
    }
  ],
  "message": "발전소 목록 조회 성공"
}
```

#### 발전소 상세 조회
```
GET /api/v1/plants/101
Authorization: Bearer <accessToken>

Response 200:
{
  "success": true,
  "data": {
    "plantId": 101,
    "name": "전북 익산 1호 발전소",
    "location": "전북 익산시",
    "capacityKw": 120.5,
    "status": "ACTIVE",
    "inverterModel": "INV-3000",
    "sensorSerialNumber": "SNSR-2026-0001"
  },
  "message": "발전소 상세 조회 성공"
}
```

## 에러 응답 예시

### 비즈니스 에러 (400)
```json
{
  "success": false,
  "error": {
    "code": "BUSINESS_ERROR",
    "message": "이미 가입된 이메일입니다.",
    "details": null
  }
}
```

### 필드 검증 에러 (400)
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "요청 값이 올바르지 않습니다.",
    "details": [
      {
        "field": "email",
        "reason": "유효한 이메일 형식이 아닙니다."
      }
    ]
  }
}
```

### 리소스 없음 (404)
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "발전소를 찾을 수 없습니다.",
    "details": null
  }
}
```

## 테스트 결과
✅ 빌드 성공
✅ 테스트 통과 (contextLoads)
✅ 모든 컴파일 에러 해결

## 다음 단계

### P1 - 대시보드 및 측정 데이터 API
- `MeasurementDto` 및 `DashboardResponse` 정리
- `GET /plants/{plantId}/dashboard/summary` 구현
- `GET /plants/{plantId}/measurements` 구현
- 시계열 데이터 조회 및 집계 로직

### P2 - 예측 및 이상 감지 API
- `Forecast`, `ForecastExplanation` 엔티티 추가
- `GET /plants/{plantId}/forecasts` 구현
- `GET /plants/{plantId}/forecasts/explanations` 구현
- 이상 탐지 상세 및 상태 변경 API

### P3 - AI 연동
- AI 클라이언트 라이브러리 추가
- 예측 요청 비동기 처리
- 이미지 분석 API
- 챗 세션/메시지

### P4 - 사용자 기능
- 알림 설정 API
- 메일 발송 통합
- 권한 세분화

## 주요 변경 사항

| 항목 | 이전 | 이후 |
|------|------|------|
| API 경로 | `/api/auth` | `/api/v1/auth` |
| 응답 형식 | 개별 DTO 직접 반환 | `ApiResponse` 래퍼 |
| 로그인 응답 | `token` 필드 | `accessToken`, `user` |
| 예외 처리 | 각각 다른 형식 | `ApiErrorResponse` 통일 |
| 발전소 정보 | 기본 필드만 | 인버터/센서/상태 추가 |

## 빌드 및 실행

### 빌드
```bash
cd C:\SpringBoot\CapstoneBackend
.\gradlew.bat clean build
```

### 테스트
```bash
.\gradlew.bat test
```

### 실행
```bash
.\gradlew.bat bootRun
```

### Swagger UI 접속
- URL: `http://localhost:8080/swagger-ui.html`

## 파일 요약
- 생성: 7개 파일 (DTO 4개, Service 2개, Controller 3개)
- 수정: 4개 파일 (Entity 1개, Service 1개, Controller 1개, SecurityConfig 1개, ExceptionHandler 1개)
- 합계: 16개 파일 영향



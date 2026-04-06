# SolarWise 백엔드 작업 정리 및 역할별 시작 가이드

## 1. 현재 코드베이스 상태 요약
- 현재 구현된 공개 엔드포인트는 `AuthController`, `DashboardController`, `AnomalyController` 3개입니다.
- 실제 경로는 `/api/auth/login`, `/api/dashboard/power-plant/{id}`, `/api/anomalies/power-plant/{id}` 입니다.
- `AiIntegrationService`, `EnergyAggregationService`는 핵심 로직이 아직 비어 있습니다.
- 엔티티는 `User`, `PowerPlant`, `EnergyLog`, `Anomaly`, `WeatherData`만 존재하며, 첨부 명세의 `Forecast`, `AlertSetting`, `ChatSession`, `VisionAnalysis` 등은 아직 없습니다.
- `./gradlew.bat test`는 기본 설정상 MySQL 연결을 요구해 실패하므로, 테스트 전용 H2 설정을 추가하는 것이 우선이었습니다.

## 2. 명세 대비 가장 큰 갭

### API 경로/응답 형식
- 명세 기준 공개 API는 `/api/v1/...` 구조입니다.
- 현재 프로젝트는 `/api/...` 구조이며 공통 응답 래퍼가 없습니다.
- 로그인 응답도 명세의 `accessToken`, `refreshToken`, `user` 구조와 다릅니다.

### 도메인 모델
- `PowerPlant`는 `capacity`를 MW 기준으로 들고 있지만 명세는 `capacityKw` 중심입니다.
- `PowerPlant`에 `status`, `inverterModel`, `sensorSerialNumber`가 없습니다.
- `EnergyLog`는 `actualGeneration`, `predictedGeneration`, `timestamp`만 있어 명세의 `temperature`, `irradiance`, `humidity`를 저장하지 못합니다.
- `Anomaly`는 `cause`, `recommendedAction`이 없고, `severity`가 `Double`이라 명세의 `LOW/MEDIUM/HIGH`와 다릅니다.

### 서비스/연동
- `AiIntegrationService`는 아직 실제 AI API 명세에 맞춘 요청 DTO/응답 DTO/에러 처리가 없습니다.
- 예측, 이미지 분석, 챗, 알림 관련 컨트롤러/서비스/리포지토리가 없습니다.
- 사용자별 발전소 접근 제어는 리포지토리 메서드만 있고 컨트롤러 단에서 충분히 쓰이지 않습니다.

## 3. 백엔드가 해야 할 일

## P0 - 바로 시작해야 하는 기반 작업
1. API 베이스 경로를 `/api/v1` 기준으로 정리
2. 공통 응답 형식(`success`, `data`, `message`, `error`) 추가
3. 테스트 프로필/H2 구성으로 로컬 테스트 안정화
4. 인증 API 확장
   - `POST /auth/signup`
   - `POST /auth/login`
   - `POST /auth/logout`
   - `GET /users/me`
5. 발전소 조회 API 구현
   - `GET /plants`
   - `GET /plants/{plantId}`
6. 대시보드/실측 데이터 API 구현
   - `GET /plants/{plantId}/dashboard/summary`
   - `GET /plants/{plantId}/measurements`
7. 엔티티/DTO 정렬
   - `PowerPlant`, `EnergyLog`, `User`, `Anomaly` 필드 재정의

## P1 - AI 연동이 시작되면 바로 붙여야 하는 작업
1. 예측 API 저장 구조 추가
   - `Forecast`, `ForecastExplanation`
2. 이상 탐지 상세/상태 변경 API
3. AI 내부 연동 클라이언트 구현
   - 발전량 예측
   - 이상 탐지
   - 이미지 분석
   - XAI 설명
4. 비동기 처리/재시도 정책 초안 수립

## P2 - 사용자 기능 완성 단계
1. 챗 세션/메시지 API
2. 알림 설정/이력 API
3. 메일 발송 정책 반영
4. Swagger 태그 정리 및 예시 응답 보강
5. 프론트엔드/AI 팀과 필드명 최종 동기화

## 4. 역할 분배 가이드(팀 공통 기준)

### 백엔드 API 영역(이승윤)에 적합한 범위
기초 CRUD + 인증 + 조회 API부터 시작하는 구성을 권장합니다.

#### 추천 담당
1. 공통 응답 DTO/에러 응답 포맷 정리
2. `User`, `PowerPlant`, `EnergyLog` 엔티티/DTO 정리
3. 회원가입/로그인/내 정보 조회 API
4. 발전소 목록/상세 조회 API
5. 실측 데이터 조회 API
6. Swagger 문서 정리
7. 기본 테스트 작성

#### 이유
- JPA 엔티티, DTO, 컨트롤러-서비스-리포지토리 흐름을 익히기에 좋습니다.
- AI 서버 비동기 연동보다 디버깅 범위가 작습니다.
- 프론트엔드 팀과 먼저 맞물리는 API를 빨리 열 수 있습니다.

### AI/데이터 연동 영역(박채리)에 적합한 범위
1. AI 서버 연동 클라이언트 설계
2. 예측/이상 탐지/이미지 분석 API 구현
3. 챗/XAI 연결
4. 알림 정책 및 비동기 처리

## 5. 백엔드 API 영역(이승윤) 시작 작업 목록
- [ ] `ApiResponse` / `ApiErrorResponse` 공통 DTO 만들기
- [ ] 인증 응답을 명세 구조(`accessToken`, `refreshToken`, `user`)로 변경
- [ ] `User`에 role enum 정리 및 회원가입 DTO 추가
- [ ] `PowerPlant` 엔티티를 명세 기준 필드로 보완
- [ ] `EnergyLog` 엔티티에 측정 지표 필드 추가
- [ ] `PlantController` 생성
- [ ] `MeasurementController` 또는 `DashboardController`를 명세형 경로로 재구성
- [ ] H2 기반 테스트에서 컨텍스트 로딩 확인

## 6. 바로 다음 구현 순서 제안
1. 공통 응답 DTO 추가
2. 인증 API 4종 완성
3. 발전소 목록/상세 조회 API 구현
4. 시계열 측정 데이터 조회 API 구현
5. 이후 예측/이상/알림/챗 확장

## 7. 이번에 먼저 시작한 것
- `docs/specs/` 폴더에 명세 문서를 프로젝트 내부로 보관
- `docs/planning/backend-work-plan.md`로 구현 갭과 역할을 문서화
- 테스트가 MySQL 없이도 돌 수 있도록 `src/test/resources/application.properties`를 추가해 기본 개발 환경 안정화를 시작


# Phase 3: AI 예측 API 연동 및 데이터 파이프라인 구축

> - **담당자**: AI/데이터 연동 영역(박채리)
> - **관련 브랜치**: `feature/ai-integration`
 
---

## 1. 개요

본 단계에서는 외부 AI 서버와 실시간으로 통신하여 **발전량 예측** 및 **예측 설명(XAI)** 데이터를 가져오는 기능을 구현했습니다. 또한, AI 모델의 학습 및 검증에 필요한 과거 기상 데이터를 대량으로 적재할 수 있는 CSV 업로드 파이프라인을 구축하여 데이터 관리의 기반을 마련했습니다.

## 2. 주요 구현 내용

### 2.1. AI 서버 통신 클라이언트 (`AiIntegrationService`)

- **`RestTemplate` 도입**: Spring의 `RestTemplate`을 사용하여 외부 Python AI 서버의 REST API를 호출하는 HTTP 클라이언트를 구현했습니다.
- **통신 로직 구현**:
    - `requestPredictionFromAi`: 발전량 예측(`POST /predict/generation`)을 요청하고 결과를 수신합니다.
    - `requestXaiExplanation`: 예측 결과에 대한 설명(`POST /explain/generation`)을 요청하고, 각 변수의 영향도(Feature Importance)를 수신합니다.
- **설정 분리**: AI 서버의 주소를 `application.properties`의 `ai.server.base-url` 속성으로 분리하여, 개발/운영 환경에 따라 쉽게 변경할 수 있도록 구성했습니다.
- **공통 로직 추출**: AI 서버 요청에 필요한 DTO를 생성하는 중복 코드를 `buildAiRequest` private 메서드로 추출하여 코드의 재사용성과 가독성을 높였습니다.

### 2.2. API 엔드포인트 개발 (`ForecastController`, `WeatherController`)

프론트엔드에서 AI 예측 기능과 데이터 업로드 기능을 사용할 수 있도록 신규 컨트롤러와 엔드포인트를 개발했습니다.

- **`ForecastController` (예측 관련)**
    - `GET /api/v1/plants/{plantId}/forecasts`: 특정 발전소의 실시간 예측 발전량을 조회합니다.
    - `GET /api/v1/plants/{plantId}/forecasts/explanations`: 예측 결과에 대한 XAI 설명을 조회합니다.
- **`WeatherController` (데이터 관련)**
    - `POST /api/v1/plants/{plantId}/weather/upload-csv`: 과거 기상 데이터가 담긴 CSV 파일을 업로드하여 DB에 저장합니다.

### 2.3. 과거 기상 데이터 CSV 대량 적재

- **`OpenCSV` 활용**: `opencsv` 라이브러리를 사용하여 대용량 CSV 파일을 스트리밍 방식으로 파싱하고, 각 행을 `WeatherData` 엔티티로 변환합니다.
- **트랜잭션 처리**: `@Transactional` 어노테이션을 적용하여 데이터 적재 과정에서 오류 발생 시 전체 작업을 롤백함으로써 데이터 정합성을 보장합니다.
- **성능 최적화**: 변환된 엔티티 리스트를 `saveAll` 메서드로 일괄 저장(Bulk Insert)하여 DB I/O 부하를 최소화했습니다.
  ※ 주의: 업로드하는 CSV 파일은 기상청 ASOS 기본 양식(기온: 4열, 습도: 6열 등)을 준수해야 합니다.
### 2.4. DTO 설계

AI 서버 및 프론트엔드와의 명확한 데이터 교환을 위해 아래와 같이 DTO(Data Transfer Object)를 신규 설계했습니다.

- **AI 서버 통신용 DTO**:
    - `AiPredictionRequest`: AI 서버에 예측을 요청할 때 보내는 데이터 (발전소 ID, 현재 기상 정보 등)
    - `AiPredictionResponse`: AI 서버로부터 받은 예측 결과 데이터 (예측 발전량, 신뢰도 등)
    - `XaiExplanationResponse`: AI 서버로부터 받은 예측 설명 데이터 (변수별 중요도, 요약 등)
    - `AiApiResponse`: AI 서버의 공통 응답 형식을 감싸는 Wrapper DTO
- **백엔드 공통 응답용 DTO**:
    - `ApiResponse`: 프론트엔드에 일관된 응답을 제공하기 위한 공통 Wrapper DTO (`success`, `error` 정적 팩토리 메서드 포함)

## 3. 파일 변경/추가 내역

| 구분 | 파일 경로 | 설명 |
 |---|---|---|
| **New** | `controller/ForecastController.java` | AI 예측/설명 API 엔드포인트 |
| **New** | `controller/WeatherController.java` | CSV 업로드 API 엔드포인트 |
| **New** | `dto/ai/*` | AI 서버 통신용 DTO 4종 |
| **New** | `dto/response/ApiResponse.java` | 백엔드 공통 응답 DTO |
| **Modified** | `service/AiIntegrationService.java` | `RestTemplate` 기반 AI 통신 및 CSV 처리 로직 구현 |
| **Modified** | `resources/application.properties` | `ai.server.base-url` 속성 추가 |

## 4. API 명세

| Method | Path | 설명 |
 |---|---|---|
| `GET` | `/api/v1/plants/{plantId}/forecasts` | AI 예측 발전량 조회 |
| `GET` | `/api/v1/plants/{plantId}/forecasts/explanations` | XAI 예측 설명 조회 |
| `POST`| `/api/v1/plants/{plantId}/weather/upload-csv` | 과거 기상 데이터 CSV 업로드 |

## 5. API 응답 예시

### 예측 발전량 조회 성공
 ```json
 {
   "success": true,
   "data": {
     "plant_id": "PLANT_001",
     "predicted_ac_power": 150.75,
     "confidence": 0.95,
     "drift_detected": false
   },
   "message": "AI 예측 발전량 조회 성공"
 }
 ```

### XAI 설명 조회 성공
 ```json
 {
   "success": true,
   "data": {
     "plant_id": "PLANT_001",
     "feature_importance": {
       "irradiation": 0.65,
       "module_temperature": 0.25,
       "ambient_temperature": 0.05,
       "wind_speed": 0.03,
       "humidity": 0.02
     },
     "summary": "일사량과 모듈 온도가 예측에 가장 큰 영향을 미쳤습니다."
   },
   "message": "XAI 예측 설명 조회 성공"
 }
 ```

## 6. 로컬 실행 및 테스트 방법

애플리케이션 실행 후, Swagger UI 또는 `curl`을 통해 테스트할 수 있습니다.

**Swagger UI**: `http://localhost:8080/swagger-ui.html`

**CURL 예시**:

 ```bash
 # 1. 예측 발전량 조회 (발전소 ID: 1)
 curl -X GET http://localhost:8080/api/v1/plants/1/forecasts
 
 # 2. 예측 설명 조회 (발전소 ID: 1)
 curl -X GET http://localhost:8080/api/v1/plants/1/forecasts/explanations
 
 # 3. CSV 파일 업로드 (발전소 ID: 1, 파일명: weather_data.csv)
 # (weather_data.csv 파일이 현재 디렉토리에 있어야 함)
 curl -X POST -F "file=@./weather_data.csv" http://localhost:8080/api/v1/plants/1/weather/upload-csv
 ```
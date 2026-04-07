# AI API 명세서

> Draft v0.1: 개발 진행에 따라 변경될 수 있음**

| 항목 | 내용 |
|---|---|
| Base URL | `http://{AI_SERVER}/api/v1` |
| AI 서버 프레임워크 | FastAPI (Python) |
| 서버 주소 | 미정 (개발 시작 시 공유) |
| 응답 형식 | JSON |
| 이미지 전송 | multipart/form-data |
| 인증 | 없음 (추후 협의) |
| 응답 시간 목표 | 예측 API 2초 이내 / 이미지 분석 5초 이내 |

---

## 공통 응답 구조

```json
{
  "status": "success",   // success | error
  "code": 200,
  "data": { ... }
}
```

---

## 1.1 발전량 예측

```
POST /predict/generation
```

**Request**
```json
{
  "plant_id": "PLANT_001",
  "datetime": "2024-03-01T14:00:00",
  "irradiation": 0.75,
  "ambient_temperature": 23.5,
  "module_temperature": 35.2,
  "wind_speed": 2.3,
  "humidity": 60.0
}
```

**Response**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "plant_id": "PLANT_001",
    "predicted_ac_power": 85.4,
    "confidence": 0.92,
    "drift_detected": false
  }
}
```

---

## 1.2 이상 탐지

```
POST /detect/anomaly
```

**Request**
```json
{
  "panel_id": "PANEL_001",
  "plant_id": "PLANT_001",
  "datetime": "2024-03-01T14:00:00",
  "actual_power": 60.0,
  "predicted_power": 100.0,
  "irradiation": 0.75,
  "ambient_temperature": 23.5,
  "module_temperature": 35.2
}
```

**Response**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "panel_id": "PANEL_001",
    "is_anomaly": true,
    "anomaly_score": 0.87,
    "severity": "HIGH",
    "recommendation": "패널 점검 필요"
  }
}
```

---

## 1.3 패널 영상 분석

```
POST /detect/image
Content-Type: multipart/form-data
```

**Request**

| 필드 | 타입 | 설명 |
|---|---|---|
| panel_id | string | 패널 ID |
| plant_id | string | 발전소 ID |
| captured_at | string | 촬영 시간 (ISO 8601) |
| image | file | jpg / png, 최대 10MB |

**Response**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "panel_id": "PANEL_001",
    "is_defective": true,
    "defect_type": "dust",
    "confidence": 0.91,
    "severity": "MEDIUM",
    "recommendation": "청소 필요"
  }
}
```

**defect_type 종류**

| 값 | 설명 |
|---|---|
| normal | 정상 |
| dust | 먼지/황사 |
| snow | 눈 |
| bird_dropping | 조류 배설물 |
| physical_damage | 파손 |

---

## 에러 응답

```json
{
  "status": "error",
  "code": 400,
  "message": "INVALID_INPUT",
  "data": {
    "field": "irradiation",
    "detail": "필수 값입니다."
  }
}
```

| 코드 | 의미 |
|---|---|
| 400 | 잘못된 입력값 |
| 500 | AI 모델 오류 |
| 503 | 모델 로딩 중 |

---

> **협의 필요:** AI 서버 주소 / 이미지 저장 방식 / 응답 시간 기준


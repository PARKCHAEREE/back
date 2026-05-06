# CSV 데이터 보관 폴더 (SolarWise 시뮬레이션 모드)

## 📌 아키텍처 변경: 가상 시간 시뮬레이션 도입

> 📍 **중요**: 이 프로젝트는 캡스톤 시연 환경입니다. 
> - **기상 API 연동 폐기**: 기상청(KMA), OpenWeather 등 외부 API 제거
> - **데이터 원본**: AI 팀이 전처리한 단 **하나의 CSV 파일만** 사용
> - **시간 제어**: `LocalDateTime.now()` 전면 금지 → `SimulationService.getVirtualCurrentTime()` 사용
> - **인메모리 시뮬레이션**: DB 스키마 변경 없이 백엔드가 가상 시간을 관리

## 폴더 구조

```
data/csv/
├── README.md              # 이 파일
├── measurements/          # 발전소별 전처리된 시계열 데이터 CSV
│   └── wooyang_merged_result.csv   # AI 팀 제공 (유일한 데이터 원본)
└── samples/               # 테스트용 샘플 CSV
```

## 파일 명명 규칙

```
wooyang_merged_result.csv
(고정 파일명 - AI 팀의 최종 전처리 버전)
```

## CSV 컬럼 설명 (AI 팀 최신 구조)

| 컬럼명 | 설명 | 단위 | 비고 |
|--------|------|------|------|
| TIME | 측정 시각 | - | 형식: `yyyy-MM-dd HH:mm` |
| ACTUAL | 실제 발전량 | kW | 실제 측정값 |
| PREDICTION | 예측 발전량 | kW | AI 사전 예측값 |
| TEMP | 기온 | °C | 환경 온도 |
| HUMI | 습도 | % | 상대 습도 |
| CLOU | 운량 | % | 구름 지수 (0~100) |
| IRRADIANCE | 일사량 | W/m² | 태양 복사 에너지 |

## 데이터 임포트 방식

### 초기 로딩 (1회)
```bash
POST /api/v1/plants/{plantId}/weather/upload-advisor-csv
Content-Type: multipart/form-data

file: wooyang_merged_result.csv
enableDemoCheat: true  # 시연용 anomaly 주입 활성화
```

### 응답 예시
```json
{
  "success": true,
  "data": {
    "totalRows": 720,
    "successCount": 720,
    "failureCount": 0
  },
  "message": "우양 CSV 데이터 적재 완료"
}
```

## 🎮 시뮬레이션 제어 API

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/v1/simulation/time` | GET | 현재 가상 시간 조회 |
| `/api/v1/simulation/tick` | POST | 가상 시간 1시간 진행 |
| `/api/v1/simulation/trigger-drone-error` | POST | 드론 이미지 오류 트리거 |

### 시뮬레이션 시간 흐름
- **시작 시각**: 2026-03-15 13:00
- **진행 방식**: 백엔드 @Scheduled 태스크가 1분마다 1시간씩 자동 진행
- **제어 방식**: `/tick` API로 수동 진행, `/trigger-drone-error`로 anomaly 주입

## CSV 업로드 방법

Swagger UI (`http://localhost:8080/swagger-ui.html`) 또는 API 직접 호출:

```http
POST /api/v1/plants/{plantId}/weather/upload-advisor-csv
Content-Type: multipart/form-data
Authorization: Bearer {JWT_TOKEN}

file: [CSV 파일 첨부]
enableDemoCheat: true  # (선택사항, 기본값: false)
```

## 주의사항

- 이 폴더의 CSV 파일들은 **Git으로 추적되지 않습니다** (`.gitignore` 처리됨)
- 실제 발전소 실측 데이터는 **외부 유출 금지** (자문가 제공 데이터)
- 파일 크기가 크면 업로드 타임아웃 발생 가능 → Spring `spring.servlet.multipart.max-file-size` 설정 확인


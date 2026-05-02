# CSV 데이터 보관 폴더

## 폴더 구조

```
data/csv/
├── README.md              # 이 파일
├── measurements/          # 발전소별 실측 시계열 데이터 CSV
│   └── KR10025001_*.csv   # 장항 태양광 발전소 (충남 서천군)
└── samples/               # 테스트용 샘플 CSV
```

## 파일 명명 규칙

```
{V_SITE_ID}_{시작일}_{종료일}.csv
예) KR10025001_20250925_20260426.csv
```

## CSV 컬럼 설명 (발전소 실측 데이터)

| 컬럼명 | 설명 | 단위 | 비고 |
|--------|------|------|------|
| V_SITE_ID | 발전소 고유 ID | - | 예: KR10025001 |
| V_TIME | 측정 시각 | - | 형식: `yyyy-MM-dd HH:mm` |
| D_PVI_CAPA | 인버터 용량 | kW | |
| D_PERIOD_GEN_KWH | 측정 구간 발전량 | kWh | 1시간 간격 |
| D_TOT_GEN_KWH | 누적 발전량 | kWh | |
| D_TEMP | 기온 | °C | 결측 시 선형 보간 |
| D_HUMIDITY | 습도 | % | 결측 시 선형 보간 |
| D_PRESSURE | 기압 | hPa | |
| D_DEW | 이슬점 | °C | |
| D_UVI | 자외선 지수 | - | 결측 시 선형 보간 |
| D_CLOUDS | 운량 | % | |
| D_VISIBILITY | 가시거리 | m | |
| D_WIND_SPEED | 풍속 | m/s | |
| D_WIND_DEG | 풍향 | ° | |
| V_GPS_Y | 위도 | ° | |
| V_GPS_X | 경도 | ° | |

## CSV 업로드 방법

Swagger UI (`http://localhost:8080/swagger-ui.html`) 또는 API 직접 호출:

```http
POST /api/v1/plants/{plantId}/measurements/upload-csv
Content-Type: multipart/form-data
Authorization: Bearer {JWT_TOKEN}

file: [CSV 파일 첨부]
```

## 주의사항

- 이 폴더의 CSV 파일들은 **Git으로 추적되지 않습니다** (`.gitignore` 처리됨)
- 실제 발전소 실측 데이터는 **외부 유출 금지** (자문가 제공 데이터)
- 파일 크기가 크면 업로드 타임아웃 발생 가능 → Spring `spring.servlet.multipart.max-file-size` 설정 확인


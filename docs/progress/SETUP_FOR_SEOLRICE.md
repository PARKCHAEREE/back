# 박채리 팀원을 위한 환경 구성 가이드

**작성일**: 2026-05-07  
**작성자**: 이승윤  
**대상**: AI/데이터 연동 담당 (박채리)

---

## 📌 현재 상황

- ✅ **feature/auth-system** 브랜치에서 RDS 통합 및 자문가 데이터 API 구현 완료
- ✅ 모든 변경사항 **develop 브랜치에 merge** 및 **푸시** 완료
- 🔄 박채리님의 **로컬 환경 동기화 및 검증 필요**

---

## 🔄 Step 1: 로컬 저장소 동기화

### 1-1) 현재 브랜치 확인
```bash
cd C:\SpringBoot\CapstoneBackend
git branch -a
```
**예상 출력:**
- `feature/auth-system`
- `feature/ai-integration`  
- `develop`
- `remot e/origin/develop`

### 1-2) develop 브랜치로 체크아웃
```bash
git checkout develop
```

### 1-3) 최신 변경사항 반영
```bash
git pull origin develop
```

**확인 사항:**
- `DAILY_LOG_2026-05-07.md` 파일이 업데이트되었는지 확인
- 커밋 메시지: "docs: 5월7일 작업로그 정정..."

---

## 🗄️ Step 2: MySQL 8.0.44 데이터베이스 확인

### 2-1) MySQL 버전 확인
```bash
mysql --version
# 또는
mysql -u root -p -e "SELECT VERSION();"
```

**확인 사항:**
- MySQL 버전이 **8.0.44** 이상인지 확인
- 만약 다른 버전이면 업그레이드 필요

### 2-2) RDS 데이터베이스 확인
```bash
mysql -h <RDS_ENDPOINT> -u <USERNAME> -p -e "show databases;"
```

**확인 사항:**
- `solarwise` 데이터베이스가 존재하는지 확인
- RDS 접속 정보는 `application-rds.properties`에서 확인

---

## ⚙️ Step 3: 환경 설정 파일 확인

### 3-1) application-rds.properties 확인
파일 위치: `src/main/resources/application-rds.properties`

**내용 예시:**
```properties
spring.datasource.url=jdbc:mysql://<RDS_ENDPOINT>:3306/solarwise
spring.datasource.username=<USERNAME>
spring.datasource.password=<PASSWORD>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

**확인 사항:**
- `<RDS_ENDPOINT>` - 실제 RDS 엔드포인트로 변경됨
- `<USERNAME>`, `<PASSWORD>` - 정확한 자격증명 입력
- `ddl-auto=update` - DB 스키마 자동 생성/업데이트

### 3-2) application.properties 확인
파일 위치: `src/main/resources/application.properties`

**내용:**
- 기본 프로필이 `rds`로 설정됨
- H2 (인메모리) 제거됨

```properties
spring.profiles.active=rds
```

---

## 🧪 Step 4: Swagger UI에서 RDS 접속 확인

### 4-1) 애플리케이션 시작
```bash
./gradlew bootRun
```

**시작 로그 확인:**
```
...
Connected to MySQL database successfully!
Tomcat started on port(s): 8080 (http)
...
```

### 4-2) Swagger UI 접속
브라우저에서 열기:
```
http://localhost:8080/swagger-ui.html
```

**확인 사항:**
- Swagger UI 정상 로딩됨
- 모든 API 엔드포인트가 표시됨

### 4-3) API 테스트 (선택사항)

#### 테스트 API 1: 회원가입
```
POST /api/v1/auth/signup
Content-Type: application/json

{
  "username": "test_user",
  "email": "test@example.com",
  "password": "TestPassword123!"
}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "test_user",
    "email": "test@example.com"
  },
  "message": "회원가입 성공"
}
```

#### 테스트 API 2: 로그인 후 발전소 목록 조회
```
1) 먼저 로그인하여 TOKEN 획득
POST /api/v1/auth/login

{
  "username": "test_user",
  "password": "TestPassword123!"
}

2) 응답에서 token 필드 복사

3) 발전소 목록 조회
GET /api/v1/plants

Headers:
Authorization: Bearer <TOKEN>
```

#### 테스트 API 3: 자문가 데이터 조회 (5월7일 신규)
```
GET /api/v1/plants/1/feature-logs/count

Headers:
Authorization: Bearer <TOKEN>
```

**예상 응답:**
```json
{
  "success": true,
  "data": 1026,
  "message": "조회 성공"
}
```

---

## 💾 Step 5: 데이터 검증

### 5-1) MySQL 터미널에서 직접 데이터 확인
```bash
mysql -h <RDS_ENDPOINT> -u <USERNAME> -p -D solarwise

# 테이블 목록 확인
SHOW TABLES;

# 데이터 건수 확인
SELECT COUNT(*) FROM plant_feature_logs;

# 데이터 샘플 확인
SELECT * FROM plant_feature_logs LIMIT 5;
```

**확인 사항:**
- `plant_feature_logs` 테이블에 1,026건의 데이터가 있는지 확인
- 최근 날짜가 `2026-04-26` 정도인지 확인

### 5-2) RDS 데이터베이스 용량 확인 (선택사항)
```sql
SELECT 
  SUM(data_length + index_length) / 1024 / 1024 / 1024 AS 'Size in GB'
FROM information_schema.TABLES 
WHERE table_schema = 'solarwise';
```

---

## 🔍 Step 6: CSV 임포트 테스트 (선택사항)

### 6-1) CSV 파일 준비
파일 위치: `data/csv/wooyang_merged_result.csv`

**CSV 컬럼 확인:**
```
TIME, ACTUAL, PREDICTION, TEMP, HUMI, CLOU, IRRADIANCE
```

### 6-2) Swagger UI에서 CSV 업로드 테스트
```
POST /api/v1/plants/1/weather/upload-advisor-csv

Parameters:
- file: wooyang_merged_result.csv
- enableDemoCheat: false

Headers:
Authorization: Bearer <TOKEN>
```

**확인 사항:**
- 업로드 성공 메시지 표시됨
- DB에 `energy_logs` 및 `weather_data` 데이터 증가

---

## 📋 Step 7: 빌드 및 테스트 검증

### 7-1) 전체 빌드 실행
```bash
./gradlew clean build
```

**예상:**
```
BUILD SUCCESSFUL in ~52s
```

### 7-2) 테스트 실행
```bash
./gradlew test
```

**예상:**
```
Test summary:
- CapstoneBackendApplicationTests ✅
- AnomalyServiceTest (7 cases) ✅
- MeasurementServiceTest ✅

BUILD SUCCESSFUL
```

---

## 🎯 Step 8: 다음 작업 관련 사항

### 8-1) 현재 상태
- ✅ RDS 통합 완료
- ✅ H2 의존성 제거
- ✅ 자문가 데이터 API 구현
- ✅ 모든 테스트 통과

### 8-2) 박채리님이 진행 중인 작업 (AI/데이터 연동)
- ✅ `AiIntegrationService` - 예측 및 이상 탐지 로직
- ✅ CSV 파이프라인 - `WeatherDataImportService`
- ✅ 비동기 처리 - `@Async`, `CompletableFuture`
- 🔄 다음: `EnergyAggregationService` 시간별/일별 데이터 집계 배치

### 8-3) 협업 포인트
| 항목 | 상태 | 담당 | 비고 |
|------|------|------|------|
| RDS 통합 | ✅ 완료 | 이승윤 | MySQL 8.0.44 기반 |
| JWT 인증 | ✅ 완료 | 이승윤 | Bearer 토큰 기반 |
| API 응답 | ✅ 완료 | 이승윤 | `ApiResponse<T>` 통일 |
| AI 클라이언트 | ✅ 완료 | 박채리 | `AiIntegrationService` |
| CSV 임포트 | ✅ 완료 | 박채리 | `WeatherDataImportService` |
| 시간 관리 | ✅ 완료 | 박채리 | `SimulationService` 가상 시간 |

---

## ⚠️ 주의사항

### MySQL 관련
- **버전**: MySQL 8.0.44 이상 필수
- **문자셋**: UTF-8 설정 필수 (한글 데이터)
- **시간대**: UTC 또는 Asia/Seoul 설정 권장

### RDS 관련
- **엔드포인트**: `application-rds.properties`에서 정확히 입력
- **보안 그룹**: 로컬 PC 또는 EC2에서 접근 가능하도록 설정
- **백업**: 프로덕션 데이터이므로 조작 시 주의

### 동시성 주의
- 이승윤: `develop` 브랜치에서 API 구현 중
- 박채리: `origin/feature/ai-integration`에서 작업 후 merge 예정
- **충돌 방지**: 같은 파일 동시 수정 금지, 정기적으로 develop 브랜치 풀

---

## 🆘 문제 해결

### 문제 1: "Connection refused" 에러
```
Cannot connect to MySQL at <RDS_ENDPOINT>:3306
```

**해결:**
1. RDS 엔드포인트와 포트 확인
2. 보안 그룹에서 3306 포트 오픈 확인
3. 자격증명 (username, password) 재확인

### 문제 2: "Access denied for user" 에러
```
Access denied for user 'root'@'...'
```

**해결:**
1. MySQL 비밀번호 확인
2. RDS에 사용자가 생성되었는지 확인
3. 권한 확인: `GRANT ALL PRIVILEGES ON solarwise.* TO 'username'@'%';`

### 문제 3: "Table doesn't exist" 에러
```
Table 'solarwise.xxx' doesn't exist
```

**해결:**
1. `spring.jpa.hibernate.ddl-auto=update` 설정 확인
2. 애플리케이션 재시작 후 테이블 자동 생성
3. 필요시 `rds_schema_v1.sql` 수동 실행

### 문제 4: 빌드 실패
```
BUILD FAILED
```

**해결:**
1. 콘솔 에러 메시지 확인
2. `./gradlew clean` 후 재시도
3. Java 버전 확인: `java -version` → 21.0.x 필수
4. 이승윤에게 보고

---

## ✅ 최종 체크리스트

박채리님께서 아래 모든 항목을 확인해주세요:

- [ ] 1-1) develop 브랜치 체크아웃 완료
- [ ] 1-3) `git pull origin develop` 실행 완료
- [ ] 2-1) MySQL 8.0.44 확인됨
- [ ] 2-2) RDS `solarwise` 데이터베이스 접근 가능
- [ ] 3-1) `application-rds.properties` 설정 확인됨
- [ ] 3-2) `application.properties`에서 `spring.profiles.active=rds` 확인됨
- [ ] 4-1) `./gradlew bootRun` 정상 시작
- [ ] 4-2) `http://localhost:8080/swagger-ui.html` 접속 가능
- [ ] 4-3) Swagger에서 회원가입/로그인 API 테스트 성공
- [ ] 5-1) MySQL에서 `plant_feature_logs` 1,026건 확인됨
- [ ] 7-1) `./gradlew clean build` 성공
- [ ] 7-2) `./gradlew test` 모든 테스트 통과

---

## 📞 연락

문제 발생 시:
1. 이 문서 다시 읽어보기
2. 콘솔 로그 및 에러 메시지 수집
3. 이승윤에게 보고

**예상 소요 시간**: 약 30분 ~ 1시간

---

**작성 완료**: 2026-05-07  
**Last Updated**: 2026-05-07


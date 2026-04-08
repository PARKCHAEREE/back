# SolarWise 백엔드 (Spring Boot 4.0.5)

태양광 발전 관리 시스템 SolarWise의 Spring Boot 백엔드 애플리케이션입니다.

## 프로젝트 개요

- **프로젝트명**: CapstoneBackend
- **버전**: 0.0.1-SNAPSHOT
- **프레임워크**: Spring Boot 4.0.5
- **Java**: 21
- **빌드**: Gradle
- **데이터베이스**: MySQL

## 핵심 기능

### 1. 사용자 인증
- 회원가입 (`POST /api/v1/auth/signup`)
- 로그인 (`POST /api/v1/auth/login`)
- 로그아웃 (`POST /api/v1/auth/logout`)
- 내 정보 조회 (`GET /api/v1/users/me`)

### 2. 발전소 관리
- 발전소 목록 조회 (`GET /api/v1/plants`)
- 발전소 상세 조회 (`GET /api/v1/plants/{plantId}`)

### 3. 대시보드 & 측정 데이터
- 대시보드 요약 조회 (`GET /api/v1/plants/{plantId}/dashboard/summary`)
- 시계열 측정 데이터 조회 (`GET /api/v1/plants/{plantId}/measurements`)

### 4. 이상 탐지
- 이상 탐지 목록 조회 (`GET /api/v1/plants/{plantId}/anomalies`)

## 빠른 시작

### 사전 요구사항
- Java 21 이상
- MySQL 5.7 이상 (또는 Docker)
- Git

### 설치 및 실행

```bash
# 1. 저장소 클론
git clone <repository-url>
cd CapstoneBackend

# 2. 데이터베이스 설정 (application.properties 수정)
# spring.datasource.url=jdbc:mysql://localhost:3306/solarwise
# spring.datasource.username=root
# spring.datasource.password=password

# 3. 빌드
./gradlew clean build

# 4. 애플리케이션 실행
./gradlew bootRun

# 5. Swagger UI 접속
# http://localhost:8080/swagger-ui.html
```

## 프로젝트 구조

```
src/main/java/com/solarwise/capstonebackend/
├── controller/     # REST API 엔드포인트
├── service/        # 비즈니스 로직
├── entity/         # JPA 엔티티
├── dto/            # 데이터 전송 객체
├── repository/     # 데이터 접근
├── security/       # JWT 인증/인가
├── exception/      # 예외 처리
└── config/         # 설정 클래스
```

## 기술 스택

### 핵심 의존성
- **Spring Security 6** - JWT 기반 인증
- **Spring Data JPA** - 데이터 접근
- **Hibernate** - ORM
- **MySQL Connector** - MySQL 연동
- **jjwt** - JWT 토큰 관리
- **SpringDoc OpenAPI** - Swagger UI 문서화
- **Lombok** - 보일러플레이트 코드 감소
- **OpenCSV** - CSV 파일 처리

### 개발 도구
- **IntelliJ IDEA** - IDE
- **Gradle** - 빌드 도구
- **JUnit 5** - 단위 테스트

## API 문서

### Swagger UI
- **URL**: `http://localhost:8080/swagger-ui.html`
- 모든 API 엔드포인트가 자동 문서화됨

### 명세 문서
- `docs/specs/API.md` - 프론트엔드 API 명세
- `docs/specs/AI_API_명세서_final.md` - AI 서버 API 명세

## 응답 형식

모든 API는 다음과 같은 공통 응답 형식을 따릅니다.

### 성공 (2xx)
```json
{
  "success": true,
  "data": { /* 실제 데이터 */ },
  "message": "처리 완료 메시지"
}
```

### 실패 (4xx, 5xx)
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": [
      {
        "field": "email",
        "reason": "유효하지 않음"
      }
    ]
  }
}
```

## 인증

모든 API (로그인/회원가입 제외)는 JWT Bearer 토큰이 필요합니다.

```bash
curl -H "Authorization: Bearer <access_token>" \
     http://localhost:8080/api/v1/users/me
```

## 개발 가이드

### 새로운 REST 엔드포인트 추가

1. **DTO 정의** (`dto/` 폴더)
   ```java
   @Data
   @Builder
   public class MyDto {
       private String field;
   }
   ```

2. **엔티티 정의** (`entity/` 폴더)
   ```java
   @Entity
   @Table(name = "my_table")
   public class MyEntity { ... }
   ```

3. **리포지토리 정의** (`repository/` 폴더)
   ```java
   public interface MyRepository extends JpaRepository<MyEntity, Long> { ... }
   ```

4. **서비스 작성** (`service/` 폴더)
   ```java
   @Service
   public class MyService {
       public MyDto getMyData(Long id) { ... }
   }
   ```

5. **컨트롤러 작성** (`controller/` 폴더)
   ```java
   @RestController
   @RequestMapping("/api/v1/my-endpoint")
   public class MyController {
       @GetMapping("/{id}")
       public ResponseEntity<ApiResponse<MyDto>> get(@PathVariable Long id) { ... }
   }
   ```

### 테스트 작성

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests MyServiceTests
```

## 데이터베이스

### MySQL 연결 설정
`src/main/resources/application.properties` 수정:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/solarwise
spring.datasource.username=root
spring.datasource.password=password
```

### 테이블 자동 생성
`spring.jpa.hibernate.ddl-auto=update` 설정으로 자동 생성/업데이트됨

## 로깅

로그 레벨 설정 (`application.properties`):
```properties
logging.level.root=INFO
logging.level.com.solarwise.capstonebackend=DEBUG
```

## 문제 해결

### MySQL 연결 실패
```bash
# MySQL 서버 상태 확인
mysql -h localhost -u root -p

# MySQL 없으면 Docker로 실행
docker run --name mysql -e MYSQL_ROOT_PASSWORD=password \
  -p 3306:3306 mysql:8.0
```

### 포트 충돌 (8080 사용 중)
```properties
# application.properties에서 포트 변경
server.port=8081
```

### 테스트 실패 (MySQL 필요)
테스트는 자동으로 H2 메모리 DB 사용
(`src/test/resources/application.properties` 참고)

## 팀 협업

### Git 브랜치 전략
```
main (배포용)
  ├── develop (통합용)
  │   ├── feature/auth-system (인증 기능)
  │   ├── feature/dashboard (대시보드)
  │   └── feature/ai-integration (AI 연동)
```

### 커밋 메시지 규칙
```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 스타일 (포맷팅 등)
refactor: 코드 리팩토링
test: 테스트 추가/수정
```

## 역할 분담

- **백엔드 API 영역(이승윤)**: 인증, 발전소 조회, 측정 데이터 API
- **AI/데이터 연동 영역(박채리)**: 예측, AI 연동, 챗 기능

자세한 내용은 `docs/planning/backend-work-plan.md` 참고

## 라이선스

프로젝트 내부 사용만 가능

## 문의

팀원들과 협의 필요 시 GitHub Issues 또는 팀 채널 참고



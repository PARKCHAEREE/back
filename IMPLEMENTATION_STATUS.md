# IMPLEMENTATION STATUS

최신 상태 요약 (2026-06-01):

## 전체 개요
- 프로젝트: CapstoneBackend (Spring Boot 4.0.5, Java 21)
- 목적: 로컬 노트북 1대로 시연 가능한 태양광 발전소 관제 대시보드 안정화 및 기능 완성

## 오늘까지 완료된 핵심 항목
1. WebFlux 제거: `build.gradle`에서 `spring-boot-starter-webflux` 의존성 제거로 WebMVC/WebFlux 충돌 위험 해소
2. `NotificationService` 네이버 SMTP 규칙 준수 구현 (발신자 주소는 `spring.mail.username` 기반으로 강제 설정)
   - 파일: C:\Users\user0725\IdeaProjects\CapstoneBackend\src\main\java\com\solarwise\capstonebackend\service\NotificationService.java
3. `SimulationService` 비즈니스 로직 수정: 이메일 발송 후 DB 상태 변경 제거, 대신 인-메모리로 발송 여부 추적
   - 파일: C:\Users\user0725\IdeaProjects\CapstoneBackend\src\main\java\com\solarwise\capstonebackend\service\SimulationService.java
4. 비동기/타임아웃 방어 코드 추가
   - `AsyncConfig` 추가(제한된 ThreadPoolTaskExecutor)
   - `WebConfig`의 `RestTemplate`에 타임아웃 설정 추가 및 CORS 제한
5. `DashboardService`에 간단한 인메모리 캐시 추가로 1초 폴링 시 DB 부하 완화

## 테스트 상태
- 최근 실행(로컬 CI)에서 ApplicationContext 로드 실패:

```
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'org.springframework.mail.javamail.JavaMailSender' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}
```

- 원인: `NotificationService`가 `JavaMailSender` 빈을 생성 시점에 주입받도록 구성되어 있으나, 테스트 프로파일 또는 테스트 환경에 최소한의 `spring.mail.*` 설정이 없어 자동 구성으로 `JavaMailSender` 빈이 생성되지 않음.

## 권장 다음 단계
1. 테스트가 성공적으로 통과하도록 테스트 전용 `JavaMailSender` 빈을 추가하거나 `src/test/resources/application.properties`(또는 application-test.properties)에 최소 `spring.mail.host`/`spring.mail.username` 설정 추가
   - 간단한 빠른 방법: `@TestConfiguration`으로 `JavaMailSender` 모킹 빈 등록
2. 인메모리 `notifiedAnomalyIds`는 프로세스 재시작 시 초기화되므로, 장기 운영을 고려하면 Redis나 DB 기반 플래그/TTL을 도입 권장
3. 시연 전 리허설 권장: 1초 폴링 및 스케줄러 동시 부하 테스트(메모리/DB 커넥션 모니터링 포함)

## 최신 커밋
- 최근 커밋(예시):

```
[feature/ai-integration f941e93] fix(simulation): track notified anomaly IDs in-memory to avoid changing DB status; add async executor; naver SMTP email formatting; remove webflux
 12 files changed, 200 insertions(+), 48 deletions(-)
```

## 비고
- 테스트 실행은 사용자가 로컬 환경에서 직접 수행하기로 하였으므로 여기서는 테스트를 수행하지 않음.


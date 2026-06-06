# AGENTS.md - AI Coding Agent Guide

## Project Overview

**CapstoneBackend** is a Spring Boot 4.0.5 backend application for a solar energy management system (SolarWise). It's built with Gradle and uses Java 21. The system provides comprehensive functionality including user authentication, solar plant management, AI-powered forecasting, anomaly detection, notifications, and real-time chat integration.

### Key Technology Stack
- **Framework**: Spring Boot 4.0.5 with Gradle
- **Java Version**: 21
- **Core Dependencies**: Spring Data JPA, Spring Security, Spring Web/WebFlux, JWT (jjwt), OpenCSV, Thymeleaf, Mail (Naver SMTP)
- **Database**: AWS RDS MySQL (production) / H2 (testing)
- **External Integration**: WebClient (for AI server calls via ngrok)
- **API Documentation**: SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- **Project Lombok** for reducing boilerplate code
- **Async & Scheduling**: `@EnableAsync` and `@EnableScheduling` enabled in main application

## Build & Test Commands

### Building
```bash
./gradlew build          # Full build and test
./gradlew bootRun        # Run application (Spring Boot dev mode, default H2 in-memory DB)
./gradlew bootRun --args='--spring.profiles.active=rds'  # Run with AWS RDS MySQL
./gradlew clean build    # Clean rebuild
```

### Testing
```bash
./gradlew test           # Run all tests with JUnit 5 (uses H2 in-memory database)
./gradlew test --info    # Run tests with detailed output
./gradlew test --tests MyServiceTests  # Run specific test class
```

### Key Gradle Configurations
- Test framework: **JUnit 5 (Jupiter)** via `useJUnitPlatform()` in build.gradle
- Mocking: **Mockito** included for unit tests via `@Mock` and `@ExtendWith(MockitoExtension.class)`
- Test Database: **H2 in-memory database** configured for isolated test execution
- Annotation processor configured for Lombok
- Spring Dependency Management plugin handles version alignment
- UTF-8 encoding enabled for both compilation and runtime

## Project Structure Conventions

```
src/main/java/com/solarwise/capstonebackend/
├── CapstoneBackendApplication.java      # Main @SpringBootApplication entry point with @EnableAsync & @EnableScheduling
├── entity/                              # JPA entity models (User, PowerPlant, Forecast, ChatSession, Anomaly, etc.)
├── repository/                          # Spring Data JPA repositories
├── service/                             # Business logic services (AuthService, PlantService, ForecastService, ChatService, etc.)
├── controller/                          # REST controllers with JWT auth
├── dto/                                 # Data transfer objects (ApiResponse<T>, ApiErrorResponse, etc.)
├── security/                            # Security configuration, JWT handling, authentication filters
├── event/                               # Domain events (e.g., ForecastGenerationEvent)
├── exception/                           # Custom exceptions & global exception handler
├── config/                              # Application configuration beans (WebConfig, WebClientConfig, AsyncConfig, etc.)
└── util/                                # Utility classes (e.g., CsvParsingUtil)

src/main/resources/
├── application.properties                                    # Default properties (shared across profiles)
├── application-rds.properties.example                       # RDS connection template
├── application-rds.properties                               # RDS configuration (production)
└── images/ & templates/                                     # Static resources if needed
```

**Current State**: Fully scaffolded with comprehensive implementation across all layers. Multiple domain entities, services, and REST endpoints already implemented.

## Authentication & Security Patterns

- **JWT Authentication**: Using `jjwt` library (v0.11.5) with:
  - `io.jsonwebtoken:jjwt-api` for token creation/validation
  - `io.jsonwebtoken:jjwt-impl` and `jjwt-jackson` at runtime
- **Spring Security 6**: Configured with `@EnableWebSecurity` and `@EnableMethodSecurity(prePostEnabled = true)` for method-level authorization
- **Stateless Session**: `SessionCreationPolicy.STATELESS` - no session cookies, JWT-only
- **JWT Filter Chain**: `JwtAuthenticationFilter` added before `UsernamePasswordAuthenticationFilter` to extract and validate Bearer tokens
- **Password Encoding**: `BCryptPasswordEncoder` for secure password hashing

### Actual Security Implementation (SecurityConfig.java pattern)
- Filter registration: `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`
- Public endpoints: `/swagger-ui/**`, `/v3/api-docs/**`, `/api/v1/auth/**`, `/api/weather/**`
- Protected endpoints: All others require `@PreAuthorize` or authenticated context
- CORS enabled with defaults
- CSRF disabled for stateless API design
- User principal lookup via `SecurityContextHolder.getContext().getAuthentication()`

## Database Integration

- **ORM**: Spring Data JPA (Hibernate)
- **Production Database**: AWS RDS MySQL via `application-rds.properties`
- **Testing Database**: H2 in-memory database (isolated test environment)
- **Profile Activation**: Use `--spring.profiles.active=rds` to switch to production MySQL
- **Configuration Files**:
  - `application.properties`: Shared configuration (Hibernate settings, logging, email, AI server URL)
  - `application-rds.properties.example`: Template for RDS connection (copy and fill in credentials)
  - `application-rds.properties`: Actual RDS configuration (exclude from version control)

### RDS Setup Example
```properties
# application-rds.properties
spring.datasource.url=jdbc:mysql://<RDS_ENDPOINT>:3306/solarwise
spring.datasource.username=<DB_USER>
spring.datasource.password=<DB_PASSWORD>
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### Features
- Automatic schema creation/updates via Hibernate (`ddl-auto=update`)
- H2 console available for testing (when using default profile)
- Query logging enabled in development (`show-sql=true`, `format_sql=true`)

## API Response Format & Error Handling

All API responses follow a standardized format for consistency and predictability:

### Success Response (2xx)
```json
{
  "success": true,
  "data": { /* actual data object */ },
  "message": "Operation completed successfully"
}
```
- Generic wrapper: `ApiResponse<T>` - provides type-safe response structure
- Use: `return ResponseEntity.ok(ApiResponse.success(data, message));`
- Located in: `dto/ApiResponse.java`

### Error Response (4xx, 5xx)
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "User-friendly error message",
    "details": [
      {
        "field": "email",
        "reason": "Invalid format"
      }
    ]
  }
}
```
- Handler: `GlobalExceptionHandler` intercepts and formats all exceptions
- Custom exceptions: `BusinessException`, `ResourceNotFoundException`
- Pattern: `throw new BusinessException("Clear message", HttpStatus.BAD_REQUEST);`

## Key Domain Entities & Relationships

| Entity | Purpose | Key Fields |
|--------|---------|-----------|
| **User** | System user account & authentication | id, email, password (hashed), name, role |
| **PowerPlant** | Solar power plant installation | id, name, capacity, location, userId (owner) |
| **Forecast** | ML-generated power forecasts | id, plantId, timestamp, predictedPower, confidence |
| **ForecastExplanation** | AI-generated explanation for forecasts | id, forecastId, explanationText |
| **Anomaly** | Detected system anomalies/issues | id, plantId, type, severity, detectedAt, status |
| **AlertSetting** | User-configured alert thresholds | id, plantId, alertType, threshold, notificationEmail |
| **ChatSession** | Conversation session with AI | id, userId, createdAt |
| **ChatMessage** | Individual message in chat | id, sessionId, role (user/assistant), content |
| **VisionAnalysis** | Visual inspection/CV analysis results | id, plantId, imageUrl, detectionResults |
| **PlantFeatureLog** | Feature vectors for ML model training | id, plantId, features (JSON), timestamp |

### Typical Data Flows for Common Operations

**User Authentication Flow:**
```
LoginRequest → AuthController → AuthService.login() 
  → UserRepository.findByEmail() → Password verification (BCrypt) 
  → JwtUtil.generateToken() → LoginResponse (with JWT token)
```

**Anomaly Detection & Alert Flow:**
```
Measurement data → AnomalyService.detect() 
  → Anomaly entity created → AlertService.checkAnomalies() 
  → NotificationService.sendEmail() (if AlertSetting threshold exceeded)
```

**Forecast Generation Flow (Event-Driven):**
```
ForecastGenerationEvent published → @EventListener in ForecastService 
  → AiIntegrationService.callAiServer() (async WebClient call to AI server) 
  → Forecast entity saved → ForecastExplanation generated 
  → Subscribers notified
```

## CSV Processing

- **Library**: `opencsv:5.7.1` - use for data import/export
- Common use case: Solar energy data ingestion (assumed from project name)
- Pattern: Create service methods with `CSVReader`/`CSVWriter` for bulk operations

## Common Agent Tasks

### Adding a New REST Endpoint
1. Create/update entity class in `entity/` with `@Entity`, `@Table(name="...")`, and Lombok annotations (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
2. Create repository interface extending `JpaRepository<Entity, ID>` in `repository/`
3. Create DTOs in `dto/` for request/response (e.g., `CreateXxxRequest`, `XxxResponse`)
4. Create service class in `service/` with `@Service` annotation
5. Create controller in `controller/` with `@RestController`, `@RequestMapping`, and controller-level `@Tag` for Swagger
6. Use `ApiResponse<T>.success(data, message)` for success responses
7. Use `@PreAuthorize` or check `SecurityContextHolder.getContext().getAuthentication()` for auth
8. Catch exceptions via `GlobalExceptionHandler` (extends with custom `@ExceptionHandler` if needed)
9. Add Swagger annotations: `@Operation(summary="...", description="...")` on methods, `@io.swagger.v3.oas.annotations.parameters.RequestBody` for clarity
10. No manual endpoint registration needed - Swagger auto-documents via annotations

### Adding Database Model
1. Place entity in `src/main/java/com/solarwise/capstonebackend/entity/`
2. Use `@Entity`, `@Table(name="snake_case_name")` annotations
3. Use Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
4. Create corresponding `JpaRepository` in `repository/` extending `JpaRepository<Entity, Long>`
5. Schema auto-creates/updates via Hibernate DDL settings in `application.properties`

### Calling External APIs (e.g., AI Server)
1. Use `WebClient` bean configured in `WebClientConfig`
2. Example: Async call to external ML API via `baseUrl` from `application.properties` (`ai.server.base-url`)
3. Use Spring's `@Async` on service methods for non-blocking operations
4. See `AiIntegrationService` for pattern: `WebClient.create().post(...).exchangeToMono(...)`

### Handling Async Tasks & Scheduling
1. Enable `@Async` and `@Scheduled` via annotations already enabled in main application
2. Create methods annotated with `@Async` in services for background execution
3. Use `@Scheduled(fixedRate/fixedDelay/cron)` for periodic tasks
4. Configure thread pools via `AsyncConfig` bean if needed
5. Example use case: Forecast generation events (`ForecastGenerationEvent`)

### Running with Custom Properties
```bash
./gradlew bootRun --args='--spring.profiles.active=rds --server.port=8081'
./gradlew bootRun --args='--spring.datasource.url=jdbc:mysql://localhost:3306/custom_db'
```

## Testing Conventions

- **Test Class Location**: `src/test/java/com/solarwise/capstonebackend/[component]Tests.java` or `[component]Test.java`
- **Unit Testing**: Use `@ExtendWith(MockitoExtension.class)` with `@Mock` and `@InjectMocks` for service/unit tests
- **Integration Testing**: Use `@SpringBootTest` for full Spring context integration tests
- **Mocking**: **Mockito** is configured and used - `@Mock`, `@InjectMocks`, `verify()`, `when()`, `ArgumentMatchers`
- **Test Database**: H2 in-memory database configured for isolated test execution
- **Assertions**: Use AssertJ (`assertThat()`, `assertThatThrownBy()`) for fluent assertions

### Unit Test Template (Service with Mocks)
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTests {
    @Mock
    private MyRepository myRepository;
    
    @Mock
    private ExternalService externalService;
    
    @InjectMocks
    private MyService myService;
    
    @Test
    void testCreateEntity() {
        // Arrange
        when(myRepository.save(any(MyEntity.class)))
            .thenReturn(new MyEntity(1L, "test"));
        
        // Act
        MyEntity result = myService.create("test");
        
        // Assert
        assertThat(result).isNotNull();
        verify(myRepository).save(any(MyEntity.class));
    }
}
```

### Integration Test Template (Controller/Full Context)
```java
@SpringBootTest
class MyControllerTests {
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void testEndpoint() {
        webTestClient.get()
            .uri("/api/v1/endpoint")
            .header("Authorization", "Bearer token")
            .exchange()
            .expectStatus().isOk();
    }
}
```

## Key Integration Points

1. **JWT Claims** ↔ **User Identification**: Extract user ID from JWT token in `SecurityContextHolder` before database queries; use `JwtUtil.extractUserId(token)` in services
2. **CSV Import** ↔ **Database**: CSV parsing via `CsvParsingUtil` → entity conversion → repository save; handle BOM-free UTF-8 CSV format
3. **External AI Services** ↔ **Forecasting**: `WebClient` calls to `ai.server.base-url` endpoint; async execution via `@Async` methods in `AiIntegrationService`
4. **Email Notifications** ↔ **Alerts**: Spring Mail integration (Naver SMTP) triggered by anomalies/alerts; use `NotificationService.sendEmail()`
5. **REST Controllers** ↔ **Services**: Controllers handle HTTP concerns and auth validation; services handle core business logic
6. **Event Publishing** ↔ **Async Processing**: Use Spring's `ApplicationEventPublisher.publishEvent()` for domain events (e.g., `ForecastGenerationEvent`) and `@EventListener` subscriber methods
7. **Scheduler** ↔ **Batch Operations**: `@Scheduled` methods run periodic tasks (e.g., data aggregation, forecast updates) - configured in `AsyncConfig`
8. **WebClient** ↔ **External APIs**: Centralized in `WebClientConfig` bean; all external calls route through single configured client

## Gotchas & Patterns to Avoid

- Don't mix `spring-boot-starter-webmvc` and `spring-boot-starter-webflux` logic carelessly - this project uses both MVC for controllers and WebFlux for `WebClient`
- JWT token expiration must be validated in filter or service, not just at decode time
- Lombok `@Data` generates `equals()` and `hashCode()` - be careful with JPA entity identity and comparisons
- CSV BOM encoding: Ensure CSV imports use **UTF-8 without BOM** (first column parsing fails with BOM prefix like `\uFEFFTIME`)
- Multipart uploads: Default file size limit is 1MB; add properties for larger files:
  ```properties
  spring.servlet.multipart.max-file-size=20MB
  spring.servlet.multipart.max-request-size=20MB
  ```
- OpenAPI generation requires proper `@RestController` annotations - plain `@Controller` won't show in Swagger
- Async pitfalls: `@Async` methods must be in a different bean/class or called from outside the same class (Spring proxy limitation)
- Profile switching: Tests use default profile (H2 DB) automatically; manual bootRun needs explicit `--spring.profiles.active=rds` for production
- External API calls: Use `WebClient` bean from `WebClientConfig` to ensure connection pooling and proper timeout handling
- RDS connection string: Explicitly specify timezone in JDBC URL to avoid date/time mismatches:
  ```
  jdbc:mysql://<endpoint>:3306/solarwise?serverTimezone=UTC
  ```
- SecurityContext: Only available in request thread context; won't work in `@Async` methods without special handling
- Event listeners: `@EventListener` methods execute synchronously by default; add `@Async` if background processing needed

## Development Notes

- **IDE**: IntelliJ IDEA recommended (`.idea/` folder present with configurations)
- **Java Version Management**: Using Gradle toolchain feature for Java 21 (no manual JDK setup needed)
- **Database Profiles**: 
  - Default (no profile): H2 in-memory for development/testing
  - `rds` profile: AWS RDS MySQL for production deployment
- **Configuration Files**:
  - `application.properties`: Shared settings (Hibernate, logging, email, AI server base URL)
  - `application-rds.properties.example`: Template for RDS setup (copy and fill credentials)
  - `application-rds.properties`: Actual RDS config (exclude from git via .gitignore)
- **External Service Integration**:
  - AI server: Configured via `ai.server.base-url` property (currently ngrok endpoint for dev)
  - Email service: Naver SMTP configured in `application.properties` (requires app password, not account password)
- **Build System**: Gradle wrapper (./gradlew) - no manual Gradle install needed
- **Encoding**: UTF-8 set as default for compilation and runtime JVM args
- **Testing**: H2 in-memory database automatically used; no profile needed for tests
- **Git Considerations**: Keep `application-rds.properties` in `.gitignore` (contains credentials)

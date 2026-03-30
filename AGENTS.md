# AGENTS.md - AI Coding Agent Guide

## Project Overview

**CapstoneBackend** is a Spring Boot 4.0.5 backend application for a solar energy management system (SolarWise). It's built with Gradle and uses Java 21.

### Key Technology Stack
- **Framework**: Spring Boot 4.0.5 with Gradle
- **Java Version**: 21
- **Core Dependencies**: Spring Data JPA, Spring Security, Spring Web/WebFlux, JWT (jjwt), OpenCSV, Thymeleaf
- **Database**: MySQL (via `mysql-connector-j`)
- **API Documentation**: SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- **Project Lombok** for reducing boilerplate code

## Build & Test Commands

### Building
```bash
./gradlew build          # Full build and test
./gradlew bootRun        # Run application (Spring Boot dev mode)
./gradlew clean build    # Clean rebuild
```

### Testing
```bash
./gradlew test           # Run all tests with JUnit 5
./gradlew test --info    # Run tests with detailed output
```

### Key Gradle Configurations
- Test framework: **JUnit 5 (Jupiter)** via `useJUnitPlatform()` in build.gradle
- Annotation processor configured for Lombok
- Spring Dependency Management plugin handles version alignment

## Project Structure Conventions

```
src/main/java/com/solarwise/capstonebackend/
├── CapstoneBackendApplication.java      # Main @SpringBootApplication entry point
├── [entities/]                          # JPA entity models (not yet created)
├── [repositories/]                      # Spring Data JPA repositories
├── [services/]                          # Business logic services
├── [controllers/]                       # REST controllers with JWT auth
├── [security/]                          # Security configuration & JWT handling
└── [config/]                            # Application configuration beans

src/main/resources/
├── application.properties                # Runtime properties (add DB config here)
└── static/                              # Static web resources (if needed)
```

**Current State**: Minimal scaffolding - most packages listed above need implementation.

## Authentication & Security Patterns

- **JWT Authentication**: Use `jjwt` library (v0.11.5) with:
  - `io.jsonwebtoken:jjwt-api` for token creation/validation
  - `io.jsonwebtoken:jjwt-impl` and `jjwt-jackson` at runtime
- **Spring Security 6**: Configured with security starters
- **Thymeleaf Security Integration**: Extra dependency included for template-level security checks

### Expected Security Implementation
- JWT token generation in authentication endpoint
- Filter chain for request validation with bearer token extraction
- User principal lookup via Spring Security context
- Consider `@EnableWebSecurity` on security config class

## Database Integration

- **ORM**: Spring Data JPA (Hibernate implicit)
- **Database**: MySQL with dedicated connector
- **Current Setup**: `application.properties` minimal - will need:
  ```properties
  spring.datasource.url=jdbc:mysql://localhost:3306/solarwise
  spring.datasource.username=root
  spring.datasource.password=***
  spring.jpa.hibernate.ddl-auto=update
  spring.jpa.show-sql=false
  ```

## API Documentation

- **Swagger UI enabled** via springdoc-openapi (version 3.0.2)
- Access at: `http://localhost:8080/swagger-ui.html`
- Ensure `@RestController`, `@GetMapping`, `@PostMapping` are properly annotated for auto-documentation
- Add `@OpenAPIDefinition` and `@Tag` annotations to controllers for clarity

## CSV Processing

- **Library**: `opencsv:5.7.1` - use for data import/export
- Common use case: Solar energy data ingestion (assumed from project name)
- Pattern: Create service methods with `CSVReader`/`CSVWriter` for bulk operations

## Common Agent Tasks

### Adding a New REST Endpoint
1. Create entity class in `entities/` with `@Entity` and Lombok `@Data`
2. Create repository extending `JpaRepository<Entity, ID>`
3. Create service with `@Service` annotation handling business logic
4. Create controller with `@RestController` and `@RequestMapping`
5. Add JWT validation via security filter or `@PreAuthorize` on methods
6. Register endpoint in Swagger (auto-documented if using standard annotations)

### Adding Database Model
1. Place entity in `src/main/java/com/solarwise/capstonebackend/entities/`
2. Use `@Entity`, `@Table(name="table_name")` annotations
3. Use Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
4. Create corresponding `JpaRepository` in `repositories/`

### Running with Custom Properties
```bash
./gradlew bootRun --args='--spring.datasource.url=jdbc:mysql://localhost:3306/solarwise'
```

## Testing Conventions

- **Test Class Location**: `src/test/java/com/solarwise/capstonebackend/[component]Tests.java`
- **Base Test Setup**: Use `@SpringBootTest` for integration tests
- **Mocking**: No explicit mocking library added yet - consider adding Mockito if needed
- **Current Example**: `CapstoneBackendApplicationTests` only verifies context loads

### Test Template
```java
@SpringBootTest
class MyServiceTests {
    @Autowired
    private MyService myService;
    
    @Test
    void testBehavior() {
        // Arrange, Act, Assert
    }
}
```

## Key Integration Points

1. **JWT Claims** ↔ **User Identification**: Extract user info from token before DB queries
2. **CSV Import** ↔ **Database**: Parse CSV files into entity objects, validate, then save via repository
3. **REST Controllers** ↔ **Services**: Controllers handle HTTP concerns; services handle logic
4. **Thymeleaf Templates**: If needed, render via controller returning template names (not likely for API-first design)

## Gotchas & Patterns to Avoid

- Don't mix `spring-boot-starter-webmvc` and `spring-boot-starter-webflux` logic carelessly - this project has both but typically use one per endpoint
- JWT token expiration must be validated in security filter, not just at decode time
- Lombok `@Data` generates `equals()` and `hashCode()` - be careful with JPA entity comparisons
- Database timezone handling: MySQL default may differ from JVM - explicitly set in connection string
- OpenAPI generation requires proper RestController annotations - plain Controller won't show in Swagger

## Development Notes

- **IDE**: IntelliJ IDEA (`.idea/` folder present)
- **Java Version Management**: Using Gradle toolchain feature for Java 21
- **No profiles configured yet**: Add `application-dev.properties`, `application-prod.properties` if needed
- **Initial DB migration**: Consider Flyway or Liquibase for schema versioning (not yet added)


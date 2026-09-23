# Migration Notes

This document records the two migrations of BankApp:

1. [Java 8 to Java 11](#java-8-to-11-migration-notes) (Spring Boot 2.1.4 -> 2.7.18)
2. [Java 11 to Java 21 / Spring Boot 4](#java-11-to-java-21--spring-boot-4-migration-notes)
   (Spring Boot 2.7.18 -> 4.1.1) - the current state of the project

## Java 8 to 11 Migration Notes

### Overview

This section summarizes the changes made to migrate the BankApp from Java 8 to Java 11 (LTS).

### Changes Made

#### 1. Build Configuration Updates

**Maven Configuration (`pom.xml`)**:
- Updated `java.version` from `1.8` to `11`
- Added `maven.compiler.release` property set to `11`
- Added `project.build.sourceEncoding` set to `UTF-8`
- Upgraded Maven plugins to Java 11-compatible versions:
  - `maven-compiler-plugin`: 3.11.0 with `<release>11</release>`
  - `maven-surefire-plugin`: 3.2.5
  - `maven-failsafe-plugin`: 3.2.5
  - `maven-enforcer-plugin`: 3.5.0 with Java 11+ requirement
  - `maven-javadoc-plugin`: 3.6.3

#### 2. Dependencies for Removed JDK Modules

**JAXB Runtime**:
- Added `org.glassfish.jaxb:jaxb-runtime:2.3.1` dependency
- Spring Boot already includes JAXB API and activation API as transitive dependencies
- No code changes required as Spring Boot handles JAXB integration

#### 3. Source Code Changes

**Swagger Migration (Springfox to SpringDoc OpenAPI)**:
- Replaced `io.springfox` dependencies with `org.springdoc:springdoc-openapi-ui:1.6.15`
- Updated controller annotations from Springfox (`@Api`, `@ApiOperation`) to SpringDoc (`@Tag`, `@Operation`)
- Rewrote `ApplicationConfig.java` to use SpringDoc configuration instead of Springfox Docket

**Test Framework Migration (JUnit 4 to JUnit 5)**:
- Updated test classes to use JUnit 5 annotations (`@Test` from `org.junit.jupiter.api`)
- Spring Boot 2.7.x includes JUnit 5 by default via `spring-boot-starter-test`

#### 4. CI/CD Updates

**GitHub Actions**:
- Created workflow file `.github/workflows/ci.yml` for Java 11 builds
- Configured to use JDK 11 with Temurin distribution
- Added Maven caching for improved build performance
- Runs compile, test, and verify steps on push and pull requests

#### 4. Runtime Environment

**Java Version**:
- Application now runs on OpenJDK 11 (Temurin distribution)
- No illegal reflective access warnings observed
- All tests pass with same functionality as Java 8 baseline

### Verification Results

#### Build and Test Status
- Maven compilation successful with Java 11
- All unit tests pass
- Spring Boot application starts correctly
- H2 database integration working
- API documentation accessible
- Spring Security configuration functional

#### Application Startup Verification (MBA-782)

**Verification Date**: December 14, 2025

**Java Runtime Used**:
```
openjdk version "11.0.29" 2025-10-21
OpenJDK Runtime Environment (build 11.0.29+7-post-Ubuntu-1ubuntu122.04)
OpenJDK 64-Bit Server VM (build 11.0.29+7-post-Ubuntu-1ubuntu122.04, mixed mode, sharing)
```

**Startup Command**: `java -jar target/bank-app-1.0.0.jar`

**Startup Results**:
- Application started successfully in approximately 5.08 seconds
- Tomcat initialized on port 8989 with context path '/bank-api'
- Spring Boot version: 2.7.18
- Hibernate ORM version: 5.6.15.Final
- H2 database console available at '/h2-console'
- Spring Security filter chain configured correctly
- Actuator endpoint exposed at '/actuator'

**Startup Log Analysis**:
- No ERROR level messages in startup logs
- One WARN message about `spring.jpa.open-in-view` being enabled by default (expected, non-critical)
- All Spring Data JPA repositories bootstrapped successfully (4 repositories found)
- HikariCP connection pool started successfully
- JPA EntityManagerFactory initialized for persistence unit 'default'

**Service Verification**:
| Endpoint | Status | Response |
|----------|--------|----------|
| `/actuator/health` | 200 OK | `{"status":"UP"}` |
| `/actuator` | 200 OK | Links to health endpoints |
| `/customers/all` | 200 OK | Empty array (expected) |
| `/swagger-ui.html` | 302 Redirect | Redirects to Swagger UI |
| `/h2-console/` | 200 OK | H2 Console HTML page |

**Conclusion**: The application starts without errors on Java 11 and all services are running correctly.

#### Performance and Compatibility
- No illegal reflective access warnings
- JAXB functionality working with added runtime dependency
- Default G1 garbage collector (Java 11 default) performing well
- TLS 1.3 support enabled by default

### Java 11 Benefits Gained

1. **Performance**: G1 garbage collector improvements and general JVM optimizations
2. **Security**: TLS 1.3 support and updated security algorithms
3. **Language Features**: Ready for future adoption of Java 9-11 language features
4. **Long-term Support**: Java 11 LTS provides extended support lifecycle

### Areas With Minimal Changes

The following areas required no or minimal modifications:
- **TLS Configuration**: Application uses Spring Boot defaults, no custom TLS setup
- **GC Logging**: No custom GC logging was configured, using Java 11 defaults
- **Module System**: Staying on classpath (not adopting JPMS modules)

### Future Considerations

1. **Optional Modernizations** (future PRs):
   - Adopt `var` keyword for local variables (Java 10+)
   - Use new HTTP Client API (Java 11+) if external HTTP calls are added
   - Consider adopting Java modules (JPMS) if project grows

2. **Monitoring**:
   - Monitor application performance in production
   - Watch for any TLS compatibility issues with external services (if added)

### Rollback Plan

If rollback to Java 8 is needed:
1. Revert `pom.xml` changes (set `java.version` back to `1.8`)
2. Remove JAXB runtime dependency
3. Update CI workflow to use Java 8
4. Revert Maven plugin versions if needed

### Migration Completion

- Java 11 build configuration
- Dependencies for removed JDK modules
- CI/CD updated to Java 11
- All tests passing
- Documentation updated
- Migration notes created

The Java 8 -> 11 migration is complete; the project has since moved on to Java 21 / Spring Boot 4 (next
section).

## Java 11 to Java 21 / Spring Boot 4 Migration Notes

### Overview (Java 21 / Boot 4)

This section summarizes the changes made to migrate BankApp from Java 11 / Spring Boot 2.7.18 to
Java 21 (LTS) / Spring Boot 4.1.1. The work was split into two pull requests:

- **PR 1** - JDK 21 toolchain only (`maven.compiler.release` 21, enforcer `[21,)`, Maven wrapper
  3.9.16, CI on JDK 21), still on Spring Boot 2.7.18. The baseline build was already green on JDK 21
  with no source changes (see `docs/spring-boot-compatibility.md`, "What happens today when the
  baseline runs on JDK 21").
- **PR 2** - Spring Boot 4.1.1, Jakarta namespace, Spring Security 7 rewrite, runtime verification,
  new tests and these docs.

Source of truth: the commit history of PR 2 (`git log ce770ef..HEAD`), the before/after evidence in
`docs/baseline/`, and the dependency checklist in `docs/spring-boot-compatibility.md`.

### Changes Made (Java 21 / Boot 4)

#### 1. Build Configuration Updates (Java 21 / Boot 4)

**Maven Configuration (`pom.xml`)**:

- `java.version` and `maven.compiler.release` set to `21`
- `maven-enforcer-plugin` 3.5.0 requires Java `[21,)`
- Maven wrapper updated to Maven 3.9.16 (`./mvnw` is the recommended build entry point)
- `spring-boot-starter-parent` upgraded from `2.7.18` to `4.1.1`; the explicit
  `maven-compiler-plugin`, `maven-surefire-plugin`, `maven-failsafe-plugin` and `maven-javadoc-plugin`
  version pins were dropped in favour of the versions managed by the Boot 4 parent
- `maven-compiler-plugin` keeps `-Xlint:all` and declares Lombok in `annotationProcessorPaths`
  (`${lombok.version}` from the parent)

**Managed versions that changed with the parent** (from `spring-boot-dependencies:4.1.1`):

| Component | Boot 2.7.18 | Boot 4.1.1 |
| ----------- | ------------- | ------------ |
| Spring Framework | 5.3.31 | 7.0.9 |
| Spring Security | 5.7.11 | 7.1.1 |
| Spring Data JPA | 2.7.18 | 2026.0.1 (release train) |
| Hibernate ORM | 5.6.15.Final | 7.4.5.Final |
| Jakarta Persistence | 2.2 (`javax.persistence`) | 3.2 (`jakarta.persistence`) |
| Jackson | 2.13.5 (`com.fasterxml.jackson`) | 3.1.5 (`tools.jackson`) |
| Tomcat | 9.0.83 | 11.0.24 (Servlet 6.1) |
| H2 | 2.1.214 | 2.4.240 |
| HikariCP | 4.0.3 | 7.0.2 |
| Logback | 1.2.12 | 1.5.38 |
| Lombok | 1.18.30 (explicit) | 1.18.46 (managed) |
| JUnit | Jupiter 5.8.2 | Jupiter 6.0.3 |
| Mockito | 4.5.1 | 5.23.0 |

#### 2. Starter and Dependency Replacements

- `spring-boot-starter-web` -> `spring-boot-starter-webmvc`.
  Boot 4 renamed the Spring MVC starter.
- (implicit in the JPA starter) -> `spring-boot-h2console` (runtime).
  Boot 4 moved the H2 console auto-configuration into its own module; without it `/h2-console/`
  disappears even with `spring.h2.console.enabled=true`.
- `org.springdoc:springdoc-openapi-ui:1.6.15` ->
  `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1`.
  springdoc 1.x is Boot 2 / `javax` only, 2.x is Boot 3, 3.x is the Boot 4 line.
  `ApplicationConfig.customOpenAPI()` and the `@Tag`/`@Operation`/`@ApiResponses` annotations are
  unchanged; Swagger UI stays at `/bank-api/swagger-ui/index.html`.
- `org.glassfish.jaxb:jaxb-runtime:2.3.8` -> removed.
  Added for the Java 8 -> 11 move; unused by application code and Hibernate 7 does not need it.
  Removing it also drops the `javax.xml.bind` 2.3.x jars that conflict with the Jakarta line.
- `spring-boot-starter-test` (single bundle) + `spring-security-test` -> `spring-boot-starter-test`
  (JUnit 6, AssertJ, Mockito, Spring Test) + `spring-boot-starter-webmvc-test` (MockMvc) +
  `spring-boot-starter-data-jpa-test` + `spring-boot-starter-security-test`.
  Boot 4 splits test support into per-technology starters.
- `spring-boot-starter-actuator`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`,
  `spring-boot-devtools`, `h2`, `lombok` -> same coordinates, versions managed by the parent.
  No change needed.

#### 3. Source Code Changes (Java 21 / Boot 4)

**Jakarta namespace rename**:

- `javax.persistence.*` -> `jakarta.persistence.*` in the 7 entity classes under `model/`
  (`Account`, `Address`, `BankInfo`, `Contact`, `Customer`, `CustomerAccountXRef`, `Transaction`;
  47 imports). No other `javax.*` package was in use (`validation`, `annotation`, `servlet`,
  `xml.bind`), so this was the only rename.

**Spring Security 7 rewrite (`config/SecurityConfig.java`)**:

- `WebSecurityConfigurerAdapter`, `authorizeRequests()`, `antMatchers()` and the non-lambda
  `and()` DSL were removed in Spring Security 6/7. `SecurityConfig` is now a plain `@Configuration`
  that exposes a `SecurityFilterChain` bean using the lambda DSL:
  `authorizeHttpRequests(a -> a.requestMatchers("/", "/h2-console/**").permitAll().anyRequest().permitAll())`,
  `csrf(csrf -> csrf.disable())`, `headers(h -> h.frameOptions(f -> f.disable()))`.
- Behaviour is unchanged: every endpoint answers anonymously (as on the baseline), CSRF is disabled
  and frame options are disabled so the H2 console renders.

**Hibernate 7 notes**:

- No mapping changes were required; `@Id @GeneratedValue(strategy = AUTO) private UUID id` now uses
  Hibernate's `UuidGenerator` and the dialect is auto-detected (no `hibernate.dialect` pin).
- Behaviour **fix**: Hibernate 5.6 mapped `UUID` ids on H2 to `BINARY(255)`, which H2 2.x zero-pads,
  so `@OneToOne` foreign keys could not be resolved on read and `GET /customers/**`,
  `POST /accounts/add/**` and `PUT /accounts/transfer/**` returned 500 on the baseline
  (`docs/baseline/README.md`, "Baseline defect"). Hibernate 7 maps `UUID` to the native H2 `UUID`
  column type (`docs/baseline/h2-column-types-jdk21-boot4.txt`), and the full
  customer -> account -> transfer round trip now succeeds (`docs/baseline/e2e-roundtrip-jdk21-boot4.out`,
  exit 0).
- `@Temporal(TemporalType.TIME)` on the `java.util.Date` fields is still valid under JPA 3.2 and
  was kept as-is.

**Jackson 3 notes**:

- Application code has no Jackson imports, so the `com.fasterxml.jackson` -> `tools.jackson`
  package rename is transparent. Request/response shapes were verified against the baseline with
  `docs/baseline/e2e-roundtrip.sh`.

**Java 21 idioms**:

- A light-touch pass over `src/main/java` adopts Java 21 idioms where they do not change behaviour;
  see the PR description for the list.

#### 4. Configuration and Runtime

- No `application.yml` changes were needed: `server.port`, `server.servlet.context-path`,
  `spring.security.user.*` and `spring.h2.console.enabled` keep their Boot 2 keys.
- `GET /actuator/health` now returns `{"groups":["liveness","readiness"],"status":"UP"}`;
  `/actuator/health/liveness` and `/actuator/health/readiness` are available. `/actuator` still
  exposes only `health`.
- Swagger UI (`/bank-api/swagger-ui/index.html`, `/swagger-ui.html` redirect), `/v3/api-docs`
  (now OpenAPI 3.1.0) and the H2 console (`/bank-api/h2-console/`, no `X-Frame-Options` header) were
  verified on JDK 21 / Boot 4 - see `docs/baseline/endpoints-jdk21-boot4.md` and the screenshots in
  `docs/baseline/screenshots/`.

#### 5. CI/CD Updates

- `.github/workflows/ci.yml` builds with Temurin JDK 21 (compile, test, verify).
- `.github/workflows/docs-ci.yml` (markdownlint + link check) is unchanged.

#### 6. Tests Added

12 new test methods (all MockMvc, `@SpringBootTest` + `@AutoConfigureMockMvc`):

- `BankingApiRegressionTest` (8, ordered): create customer (201), get customer by number (200 with
  details), list all customers (200 array), create two accounts (201), get account (302 with JSON
  body - baseline shape kept on purpose), transfer updates balances, list transactions (200),
  insufficient funds (400).
- `SecurityConfigTest` (4): root is neither 401 nor 403, H2 console reachable without credentials,
  POST without CSRF token is not rejected, frames are not denied.

Together with the existing `BankingApplicationTests` context load this is 13 tests; all pass with
`./mvnw -B clean verify` on JDK 21 (`docs/baseline/verify-jdk21-boot4.out` records the Boot 4 build
before the new tests were added).

### Verification Results (Java 21 / Boot 4)

- `./mvnw -B clean verify` (JDK 21, Boot 4.1.1): **BUILD SUCCESS** - evidence
  ``docs/baseline/verify-jdk21-boot4.out``
- `docs/baseline/e2e-roundtrip.sh`: **exit 0 (baseline exited non-zero)** - evidence
  ``docs/baseline/e2e-roundtrip-jdk21-boot4.out``
- UUID id columns in H2: **`UUID` (baseline `BINARY(255)`)** - evidence
  ``docs/baseline/h2-column-types-jdk21-boot4.txt``
- Swagger UI / api-docs / H2 console / Actuator: **all 200** - evidence
  ``docs/baseline/endpoints-jdk21-boot4.md``
- `jdeprscan --release 21`, `jdeps --jdk-internals` on `target/classes`: **no findings** - evidence
  ``docs/baseline/jdeprscan-jdk21.out`, `docs/baseline/jdeps-jdk-internals-jdk21.out``

### Rollback Plan (Java 21 / Boot 4)

If rollback to Java 11 / Spring Boot 2.7.18 is needed, revert PR 2 and then PR 1 (or check out the
pre-migration commit `ce770ef`). The two PRs are independent enough that PR 1 alone (JDK 21 on Boot
2.7.18) is also a valid intermediate state.

### Future Considerations (Java 21 / Boot 4)

- `spring.jpa.open-in-view` still warns at startup; set it to `false` explicitly once confirmed
  behaviour-neutral for the API.
- Consider `java.time` types instead of `java.util.Date` + `@Temporal` on the entity date fields.
- `GET /accounts/{accountNumber}` returns `302 FOUND` with a JSON body; kept for compatibility, but
  a `200 OK` would be the conventional response.
- `docs-ci.yml` currently lints only `CONTRIBUTING.md` (and the now-removed `README_NEW.md`);
  point it at `README.md`, `MIGRATION_NOTES.md` and `docs/**/*.md`.

# Spring Boot Compatibility Analysis: Java 11 Migration

## Executive Summary

This document analyzes the compatibility of Spring Boot 2.1.4.RELEASE with Java 11 and provides a detailed upgrade path to Spring Boot 2.7.18 (the latest 2.x LTS version). The analysis covers breaking changes, affected dependencies, and recommended migration strategies.

## Current Project Configuration

The BankApp project currently uses:

| Component | Current Version |
|-----------|-----------------|
| Spring Boot | 2.1.4.RELEASE |
| Java | 1.8 |
| Spring Framework | 5.1.6.RELEASE (managed by Spring Boot) |
| H2 Database | 1.4.199 (managed by Spring Boot) |
| Lombok | 1.18.6 (managed by Spring Boot) |
| Springfox Swagger | 2.9.2 / 2.10.0 |

## Java 11 Compatibility Analysis

### Spring Boot 2.1.x and Java 11

Spring Boot 2.1 (released October 2018) was the first version to officially support Java 11. According to the official Spring Boot 2.1 Release Notes:

> "Spring Boot 2.1 remains compatible with Java 8 but now also supports Java 11. We have continuous integration configured to build and test Spring Boot against the latest Java 11 release."

**Key Finding**: Spring Boot 2.1.4.RELEASE is compatible with Java 11. However, upgrading to Spring Boot 2.7.18 is recommended for the following reasons:

1. Spring Boot 2.1.x reached end of OSS support in October 2019
2. Spring Boot 2.7.x provides better Java 11 optimization and security patches
3. Spring Boot 2.7.x includes important dependency updates and bug fixes
4. Spring Boot 2.7.x has enterprise support until June 2029

### Minimum Spring Boot Version for Java 11

| Java Version | Minimum Spring Boot Version | Recommended Version |
|--------------|----------------------------|---------------------|
| Java 11 | 2.1.0.RELEASE | 2.7.18 |
| Java 17 | 2.5.0.RELEASE | 3.x (requires migration) |
| Java 21 | 3.1.0 | 3.4.x |

## Upgrade Path: Spring Boot 2.1.4 to 2.7.18

### Recommended Approach

While a direct upgrade from 2.1.4 to 2.7.18 is technically possible, we recommend an incremental approach to minimize risk and simplify troubleshooting. The upgrade can be performed in phases:

**Phase 1**: 2.1.4 -> 2.3.x (consolidate early changes)
**Phase 2**: 2.3.x -> 2.5.x (address validation and testing changes)
**Phase 3**: 2.5.x -> 2.7.18 (final upgrade with H2 and dependency updates)

Alternatively, a direct upgrade to 2.7.18 can be performed if comprehensive testing is available.

## Breaking Changes by Version

### Spring Boot 2.2 (November 2019)

**Key Changes:**
- JUnit 5 becomes the default testing framework
- Lazy initialization support added (`spring.main.lazy-initialization=true`)
- Jakarta EE dependencies updated
- Spring Framework 5.2 upgrade

**Impact on BankApp:**
- Test classes may need `@ExtendWith(SpringExtension.class)` instead of `@RunWith(SpringRunner.class)`
- JUnit 4 assertions should be migrated to JUnit 5

### Spring Boot 2.3 (May 2020)

**Key Changes:**
- Validation starter (`spring-boot-starter-validation`) no longer included in web starters
- Graceful shutdown support added
- Liveness and Readiness probes for Kubernetes
- Java 14 support added

**Impact on BankApp:**
- Must explicitly add `spring-boot-starter-validation` dependency if using `@Valid`, `@NotNull`, etc.
- Review any validation annotations in DTOs and entities

**Required Action:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Spring Boot 2.4 (November 2020)

**Key Changes:**
- Config file processing completely rewritten
- `spring.config.use-legacy-processing=true` available for backward compatibility
- JUnit 5's Vintage Engine removed from `spring-boot-starter-test`
- Java 15 support added
- New `spring.config.import` property for importing additional config files

**Impact on BankApp:**
- Review `application.properties` for any multi-document YAML or profile-specific configurations
- JUnit 4 tests will require explicit Vintage Engine dependency

**Required Action (if using JUnit 4 tests):**
```xml
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.hamcrest</groupId>
            <artifactId>hamcrest-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### Spring Boot 2.5 (May 2021)

**Key Changes:**
- SQL script-based DataSource initialization redesigned
- Hibernate Validator 6.2 upgrade
- Gradle 7 support
- Java 16 support added
- Environment variable prefixes support

**Impact on BankApp:**
- If using `schema.sql` or `data.sql`, review initialization behavior
- `spring.sql.init.mode` replaces `spring.datasource.initialization-mode`
- `spring.sql.init.platform` replaces `spring.datasource.platform`

### Spring Boot 2.6 (November 2021)

**Key Changes:**
- Circular references prohibited by default
- `PathPatternParser` is now the default for Spring MVC (instead of `AntPathMatcher`)
- Actuator env endpoint masks additional keys by default
- Java 17 support added

**Impact on BankApp:**
- Review application for circular bean dependencies
- If using Springfox Swagger, path matching may break (see Swagger section below)

**Required Action (if circular references exist):**
```properties
spring.main.allow-circular-references=true
```

**Required Action (for Springfox compatibility):**
```properties
spring.mvc.pathmatch.matching-strategy=ant_path_matcher
```

### Spring Boot 2.7 (May 2022)

**Key Changes:**
- H2 upgraded to 2.1.120 (backwards incompatible)
- OkHttp upgraded from 3.x to 4.x
- Flyway upgraded to 8.5
- Metric tag keys renamed (camelCase to lower-case with dots)
- `@SpringBootTest` property source precedence changed
- Elasticsearch `RestHighLevelClient` deprecated

**Impact on BankApp:**
- H2 database syntax changes may affect SQL scripts
- H2 console URL and settings may need adjustment

**H2 2.x Migration Notes:**
- H2 2.x is not fully backward compatible with H2 1.x
- Some SQL syntax has changed
- Default settings have changed
- Review the [H2 migration guide](https://www.h2database.com/html/migration-to-v2.html)

## Transitive Dependencies Analysis

### Dependencies Affected by Upgrade

| Dependency | Version in 2.1.4 | Version in 2.7.18 | Breaking Changes |
|------------|------------------|-------------------|------------------|
| Spring Framework | 5.1.6 | 5.3.31 | Minor API changes |
| Hibernate | 5.3.9 | 5.6.15 | Some deprecated APIs removed |
| H2 Database | 1.4.199 | 2.1.214 | **Major** - SQL syntax changes |
| Tomcat | 9.0.17 | 9.0.83 | Minor configuration changes |
| Jackson | 2.9.8 | 2.13.5 | Some deprecated methods removed |
| Lombok | 1.18.6 | 1.18.30 | Compatible |
| Micrometer | 1.1.4 | 1.9.17 | Metric naming conventions |

### Springfox Swagger Migration

**Critical Issue**: Springfox Swagger (2.9.2/2.10.0) is largely unmaintained and has compatibility issues with Spring Boot 2.6+.

**Recommended Action**: Migrate from Springfox to SpringDoc OpenAPI.

**Migration Steps:**

1. Remove Springfox dependencies:
```xml
<!-- Remove these -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
</dependency>
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
</dependency>
```

2. Add SpringDoc OpenAPI:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

3. Update Swagger configuration class (if any)
4. Access Swagger UI at `/swagger-ui.html` or `/swagger-ui/index.html`

**Alternative (Temporary)**: Keep Springfox with path matching workaround:
```properties
spring.mvc.pathmatch.matching-strategy=ant_path_matcher
```

## Risk Assessment

### High Risk Items

1. **H2 Database Upgrade (2.1.x to 2.x)**
   - SQL syntax changes
   - Default behavior changes
   - May require schema/data script modifications

2. **Springfox Swagger Compatibility**
   - Incompatible with Spring Boot 2.6+ default path matching
   - Requires workaround or migration to SpringDoc

### Medium Risk Items

1. **Circular Reference Detection**
   - Spring Boot 2.6+ prohibits circular references by default
   - May require code refactoring or configuration override

2. **Validation Starter Removal**
   - Must explicitly add validation starter in 2.3+
   - Missing dependency will cause runtime errors

3. **JUnit 5 Migration**
   - Test classes may need updates
   - Vintage engine required for JUnit 4 tests

### Low Risk Items

1. **Config File Processing Changes**
   - Legacy processing mode available
   - Most simple configurations unaffected

2. **Metric Tag Key Renaming**
   - Only affects monitoring/alerting configurations
   - Can be customized if needed

## Recommended Upgrade Steps

### Pre-Upgrade Checklist

- [ ] Ensure all tests pass on current version
- [ ] Document current application behavior
- [ ] Review and backup database schemas
- [ ] Identify all circular bean dependencies
- [ ] Inventory all validation annotations usage
- [ ] Review Swagger/OpenAPI configuration

### Upgrade Procedure

1. **Update pom.xml**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<properties>
    <java.version>11</java.version>
</properties>
```

2. **Add Required Dependencies**
```xml
<!-- Validation (required since 2.3) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

3. **Update application.properties**
```properties
# For Springfox compatibility (temporary)
spring.mvc.pathmatch.matching-strategy=ant_path_matcher

# If circular references exist (temporary)
spring.main.allow-circular-references=true
```

4. **Migrate Swagger (Recommended)**
   - Replace Springfox with SpringDoc OpenAPI
   - Update API documentation annotations

5. **Update Tests**
   - Migrate to JUnit 5 assertions
   - Update test annotations

6. **Verify H2 Compatibility**
   - Test all database operations
   - Update SQL scripts if needed

## Post-Upgrade Validation

- [ ] Application starts successfully
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Swagger UI accessible and functional
- [ ] H2 console accessible
- [ ] All REST endpoints functional
- [ ] Actuator endpoints working
- [ ] No deprecation warnings in logs

## Timeline Estimate

| Phase | Description | Estimated Effort |
|-------|-------------|------------------|
| 1 | Dependency updates and configuration | 2-4 hours |
| 2 | Swagger migration (if applicable) | 2-4 hours |
| 3 | Test migration and fixes | 4-8 hours |
| 4 | H2 compatibility testing | 2-4 hours |
| 5 | Integration testing | 4-8 hours |
| **Total** | | **14-28 hours** |

## References

- [Spring Boot 2.1 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.1-Release-Notes)
- [Spring Boot 2.2 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.2-Release-Notes)
- [Spring Boot 2.3 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.3-Release-Notes)
- [Spring Boot 2.4 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.4-Release-Notes)
- [Spring Boot 2.5 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.5-Release-Notes)
- [Spring Boot 2.6 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.6-Release-Notes)
- [Spring Boot 2.7 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.7-Release-Notes)
- [Spring Boot Support Policy](https://spring.io/projects/spring-boot#support)
- [H2 Database Migration Guide](https://www.h2database.com/html/migration-to-v2.html)
- [SpringDoc OpenAPI Migration Guide](https://springdoc.org/migrating-from-springfox.html)

## Conclusion

Spring Boot 2.1.4.RELEASE is compatible with Java 11, making the Java version upgrade straightforward. However, upgrading to Spring Boot 2.7.18 is strongly recommended to benefit from security patches, bug fixes, and improved Java 11 support.

The main areas requiring attention during the upgrade are:
1. Springfox Swagger migration or workaround
2. H2 database compatibility
3. Validation starter dependency
4. Circular reference handling
5. JUnit 5 test migration

With proper planning and testing, the upgrade can be completed with minimal disruption to the application.

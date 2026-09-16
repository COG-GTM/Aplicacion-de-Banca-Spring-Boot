# Springfox to SpringDoc OpenAPI Migration Guide

## Overview

This document provides a comprehensive analysis and migration guide for transitioning from Springfox Swagger to SpringDoc OpenAPI in the BankApp project. This migration is part of the Java 8 to Java 11 upgrade initiative (MBA-766) and is necessary because Springfox is no longer actively maintained and has compatibility issues with Spring Boot 2.6+ and Java 11+.

## Executive Summary

The BankApp project currently uses Springfox Swagger (versions 2.9.2/2.10.0) for API documentation. SpringDoc OpenAPI is the recommended modern alternative that provides full OpenAPI 3.0 specification support, active maintenance, and seamless compatibility with Spring Boot 2.7.x and Java 11. The migration requires updating dependencies, modifying the configuration class, and updating controller annotations.

**Estimated Effort**: Low to Medium (2-4 hours for implementation)

**Risk Level**: Low (annotation changes are straightforward with clear mappings)

## Current State Analysis

### Dependencies (pom.xml)

The project currently uses two Springfox dependencies:

```xml
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>2.9.2</version>
</dependency>
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>2.10.0</version>
</dependency>
```

### Configuration (ApplicationConfig.java)

The current Swagger configuration uses the Docket-based approach:

```java
@Configuration
@EnableSwagger2
public class ApplicationConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .paths(PathSelectors.any())
                .build();
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("BANKING APPLICATION REST API")
                .description("API for Banking Application.")
                .version("1.0.0").build();
    }
}
```

### Annotations Used in Controllers

The following Springfox annotations are currently used in the codebase:

| Annotation | Location | Count | Purpose |
|------------|----------|-------|---------|
| `@Api` | AccountController, CustomerController | 2 | Groups endpoints under tags |
| `@ApiOperation` | All controller methods | 9 | Describes individual operations |
| `@ApiResponse` | All controller methods | 27 | Documents response codes |
| `@ApiResponses` | All controller methods | 9 | Container for multiple responses |

**Note**: No `@ApiModel` or `@ApiModelProperty` annotations are used in the domain classes, which simplifies the migration.

### Files Requiring Changes

1. `pom.xml` - Dependency updates
2. `src/main/java/com/coding/exercise/bankapp/config/ApplicationConfig.java` - Configuration rewrite
3. `src/main/java/com/coding/exercise/bankapp/controller/AccountController.java` - Annotation updates
4. `src/main/java/com/coding/exercise/bankapp/controller/CustomerController.java` - Annotation updates

## SpringDoc OpenAPI Overview

SpringDoc OpenAPI is the modern successor to Springfox for Spring Boot applications. Key advantages include:

- **Active Maintenance**: Regular updates and bug fixes
- **OpenAPI 3.0 Support**: Full compliance with the latest OpenAPI specification
- **Spring Boot 2.x/3.x Compatibility**: Works seamlessly with modern Spring Boot versions
- **Java 11+ Support**: No compatibility issues with newer Java versions
- **Auto-configuration**: Minimal configuration required
- **Swagger UI Integration**: Built-in Swagger UI with modern features

## Annotation Mapping Reference

### Controller-Level Annotations

| Springfox (io.swagger.annotations) | SpringDoc (io.swagger.v3.oas.annotations) | Notes |
|------------------------------------|-------------------------------------------|-------|
| `@Api(tags = {"..."})` | `@Tag(name = "...")` | Tag name for grouping |
| `@ApiIgnore` | `@Hidden` | Hide from documentation |

### Method-Level Annotations

| Springfox | SpringDoc | Notes |
|-----------|-----------|-------|
| `@ApiOperation(value = "...", notes = "...")` | `@Operation(summary = "...", description = "...")` | Operation description |
| `@ApiResponses({...})` | `@ApiResponses({...})` | Same name, different package |
| `@ApiResponse(code = 200, message = "...")` | `@ApiResponse(responseCode = "200", description = "...")` | Note: `code` becomes `responseCode`, `message` becomes `description` |

### Parameter Annotations

| Springfox | SpringDoc | Notes |
|-----------|-----------|-------|
| `@ApiParam(value = "...", required = true)` | `@Parameter(description = "...", required = true)` | Parameter documentation |
| `@ApiImplicitParam` | `@Parameter` | Implicit parameters |
| `@ApiImplicitParams` | `@Parameters` | Multiple parameters |

### Model Annotations (Not Used in This Project)

| Springfox | SpringDoc | Notes |
|-----------|-----------|-------|
| `@ApiModel(value = "...")` | `@Schema(name = "...")` | Model documentation |
| `@ApiModelProperty(value = "...")` | `@Schema(description = "...")` | Property documentation |

## Migration Steps

### Step 1: Update Dependencies (pom.xml)

Remove the Springfox dependencies and add SpringDoc OpenAPI:

```xml
<!-- REMOVE these dependencies -->
<!--
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>2.9.2</version>
</dependency>
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>2.10.0</version>
</dependency>
-->

<!-- ADD this dependency -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

**Note**: Version 1.7.0 is the latest stable version compatible with Spring Boot 2.x. For Spring Boot 3.x, use `springdoc-openapi-starter-webmvc-ui` version 2.x.

### Step 2: Update Configuration (ApplicationConfig.java)

Replace the Docket-based configuration with SpringDoc's OpenAPI configuration:

**Before (Springfox):**
```java
package com.coding.exercise.bankapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class ApplicationConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .paths(PathSelectors.any())
                .build();
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("BANKING APPLICATION REST API")
                .description("API for Banking Application.")
                .version("1.0.0").build();
    }
}
```

**After (SpringDoc):**
```java
package com.coding.exercise.bankapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class ApplicationConfig {

    @Bean
    public OpenAPI bankingApplicationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BANKING APPLICATION REST API")
                        .description("API for Banking Application.")
                        .version("1.0.0"));
    }
}
```

### Step 3: Update AccountController.java

**Before (Springfox):**
```java
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("accounts")
@Api(tags = { "Accounts and Transactions REST endpoints" })
public class AccountController {

    @GetMapping(path = "/{accountNumber}")
    @ApiOperation(value = "Get account details", notes = "Find account details by account number")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error") })
    public ResponseEntity<Object> getByAccountNumber(@PathVariable Long accountNumber) {
        return bankingService.findByAccountNumber(accountNumber);
    }
    // ... other methods
}
```

**After (SpringDoc):**
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("accounts")
@Tag(name = "Accounts and Transactions REST endpoints")
public class AccountController {

    @GetMapping(path = "/{accountNumber}")
    @Operation(summary = "Get account details", description = "Find account details by account number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    public ResponseEntity<Object> getByAccountNumber(@PathVariable Long accountNumber) {
        return bankingService.findByAccountNumber(accountNumber);
    }
    // ... other methods
}
```

### Step 4: Update CustomerController.java

Apply the same annotation changes as AccountController:

**Before (Springfox):**
```java
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("customers")
@Api(tags = { "Customer REST endpoints" })
public class CustomerController {

    @GetMapping(path = "/all")
    @ApiOperation(value = "Find all customers", notes = "Gets details of all the customers")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error") })
    public List<CustomerDetails> getAllCustomers() {
        return bankingService.findAll();
    }
    // ... other methods
}
```

**After (SpringDoc):**
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("customers")
@Tag(name = "Customer REST endpoints")
public class CustomerController {

    @GetMapping(path = "/all")
    @Operation(summary = "Find all customers", description = "Gets details of all the customers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    public List<CustomerDetails> getAllCustomers() {
        return bankingService.findAll();
    }
    // ... other methods
}
```

### Step 5: Update application.yml (Optional)

Add SpringDoc configuration to customize the Swagger UI path if needed:

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha
```

## URL Changes

After migration, the Swagger UI and API documentation URLs will change:

| Resource | Springfox URL | SpringDoc URL |
|----------|---------------|---------------|
| Swagger UI | `/bank-api/swagger-ui.html` | `/bank-api/swagger-ui/index.html` or `/bank-api/swagger-ui.html` (redirects) |
| API Docs (JSON) | `/bank-api/v2/api-docs` | `/bank-api/v3/api-docs` |
| API Docs (YAML) | N/A | `/bank-api/v3/api-docs.yaml` |

## Security Configuration Considerations

The current `SecurityConfig.java` may need updates to allow access to SpringDoc endpoints. Add the following paths to the security configuration:

```java
@Override
protected void configure(HttpSecurity httpSecurity) throws Exception {
    httpSecurity.authorizeRequests()
            .antMatchers("/").permitAll()
            .antMatchers("/h2-console/**").permitAll()
            // Add SpringDoc paths
            .antMatchers("/swagger-ui/**").permitAll()
            .antMatchers("/swagger-ui.html").permitAll()
            .antMatchers("/v3/api-docs/**").permitAll()
            .and();

    httpSecurity.csrf().disable();
    httpSecurity.headers().frameOptions().disable();
}
```

## Complete Code Changes Summary

### Files to Modify

| File | Changes Required |
|------|------------------|
| `pom.xml` | Remove Springfox deps, add SpringDoc dep |
| `ApplicationConfig.java` | Replace Docket with OpenAPI bean, remove @EnableSwagger2 |
| `AccountController.java` | Update 4 imports, 1 class annotation, 4 method annotations |
| `CustomerController.java` | Update 4 imports, 1 class annotation, 5 method annotations |
| `SecurityConfig.java` | Add SpringDoc paths to permitAll() |
| `application.yml` | (Optional) Add SpringDoc configuration |

### Import Changes Summary

**Remove:**
```java
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
```

**Add:**
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
```

## Testing the Migration

After completing the migration, verify the following:

1. **Build Success**: Run `mvn clean compile` to ensure no compilation errors
2. **Test Success**: Run `mvn test` to ensure all tests pass
3. **Swagger UI Access**: Navigate to `http://localhost:8989/bank-api/swagger-ui/index.html`
4. **API Documentation**: Verify all endpoints are documented correctly
5. **Try It Out**: Test API calls through Swagger UI

## Potential Issues and Solutions

### Issue 1: Swagger UI Not Loading

**Cause**: Security configuration blocking access

**Solution**: Ensure SpringDoc paths are added to `permitAll()` in SecurityConfig

### Issue 2: Missing Endpoints in Documentation

**Cause**: Controller not being scanned

**Solution**: Verify `@RestController` annotation and package scanning configuration

### Issue 3: Response Type Not Showing

**Cause**: Generic `ResponseEntity<Object>` return types

**Solution**: Consider using specific return types or add `@Schema` annotations to document response types explicitly

### Issue 4: Spring Boot Version Incompatibility

**Cause**: Using wrong SpringDoc version for Spring Boot version

**Solution**: 
- Spring Boot 2.x: Use `springdoc-openapi-ui` version 1.x
- Spring Boot 3.x: Use `springdoc-openapi-starter-webmvc-ui` version 2.x

## Additional Enhancements (Optional)

After the basic migration, consider these enhancements:

### Add Contact and License Information

```java
@Bean
public OpenAPI bankingApplicationOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("BANKING APPLICATION REST API")
                    .description("API for Banking Application.")
                    .version("1.0.0")
                    .contact(new Contact()
                            .name("Development Team")
                            .email("dev@example.com"))
                    .license(new License()
                            .name("Apache 2.0")
                            .url("http://www.apache.org/licenses/LICENSE-2.0")));
}
```

### Add Server Information

```java
@Bean
public OpenAPI bankingApplicationOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("BANKING APPLICATION REST API")
                    .description("API for Banking Application.")
                    .version("1.0.0"))
            .servers(List.of(
                    new Server().url("http://localhost:8989/bank-api").description("Local Development"),
                    new Server().url("https://api.example.com/bank-api").description("Production")));
}
```

### Document Request/Response Schemas

Add `@Schema` annotations to domain classes for better documentation:

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Customer details information")
public class CustomerDetails {

    @Schema(description = "Customer's first name", example = "John")
    private String firstName;
    
    @Schema(description = "Customer's last name", example = "Doe")
    private String lastName;
    
    // ... other fields
}
```

## References

- [SpringDoc OpenAPI Official Documentation](https://springdoc.org/)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [Springfox to SpringDoc Migration Guide](https://springdoc.org/#migrating-from-springfox)
- [Spring Boot 2.7.x Documentation](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)

## Conclusion

The migration from Springfox to SpringDoc OpenAPI is straightforward for this project due to the limited use of Swagger annotations (only `@Api`, `@ApiOperation`, `@ApiResponse`, and `@ApiResponses`). The absence of `@ApiModel` and `@ApiModelProperty` annotations in the domain classes further simplifies the migration. Following this guide will ensure a smooth transition to SpringDoc OpenAPI with full compatibility for Java 11 and Spring Boot 2.7.x.

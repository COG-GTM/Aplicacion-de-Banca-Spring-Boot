# Java 11 Migration - Deprecated API Audit Report

**Project:** BankApp (java-migration-8-11)  
**Audit Date:** December 14, 2025  
**Current Java Version:** 1.8 (OpenJDK 8u462)  
**Target Java Version:** 11 (LTS)  
**Jira Task:** MBA-768

## Executive Summary

This audit identifies deprecated APIs, removed JDK modules, and JDK internal API usage that may impact the migration from Java 8 to Java 11. The analysis was performed using `jdeps` and `jdeprscan` tools, along with manual code inspection.

**Key Findings:**
- No direct usage of removed Java EE modules (JAXB, JAX-WS, CORBA, JavaFX, Nashorn) in application code
- JAXB dependencies are already included transitively via Spring Boot Data JPA
- Several third-party dependencies use JDK internal APIs that may trigger warnings
- One dependency (logback-classic 1.2.3) uses a removed JDK internal API

## 1. Removed JDK Modules Analysis

### 1.1 JAXB (Java Architecture for XML Binding)

**Status:** Not directly used in application code

**Dependency Analysis:**
The project includes JAXB as a transitive dependency through `spring-boot-starter-data-jpa`:
```
javax.xml.bind:jaxb-api:jar:2.3.1:compile
javax.activation:javax.activation-api:jar:1.2.0:compile
```

**Risk Level:** LOW  
**Remediation:** No action required. Spring Boot 2.1.4 already includes the necessary JAXB dependencies for Java 11 compatibility.

### 1.2 JAX-WS (Java API for XML Web Services)

**Status:** Not used

**Risk Level:** NONE  
**Remediation:** No action required.

### 1.3 CORBA (Common Object Request Broker Architecture)

**Status:** Not used

**Risk Level:** NONE  
**Remediation:** No action required.

### 1.4 JavaFX

**Status:** Not used

**Risk Level:** NONE  
**Remediation:** No action required.

### 1.5 Nashorn JavaScript Engine

**Status:** Not used

**Risk Level:** NONE  
**Remediation:** No action required.

### 1.6 Java EE Annotations (javax.annotation)

**Status:** Used transitively

**Dependency Analysis:**
```
javax.annotation:javax.annotation-api:jar:1.3.2:compile
```

**Risk Level:** LOW  
**Remediation:** Already included as explicit dependency via Spring Boot.

### 1.7 Java Transaction API

**Status:** Used transitively

**Dependency Analysis:**
```
javax.transaction:javax.transaction-api:jar:1.3:compile
```

**Risk Level:** LOW  
**Remediation:** Already included as explicit dependency via Spring Boot Data JPA.

## 2. JDK Internal API Usage (jdeps Analysis)

The following dependencies use JDK internal APIs that may trigger illegal reflective access warnings on Java 11:

### 2.1 Critical - Removed Internal API

| Dependency | Internal API | Status | Risk |
|------------|--------------|--------|------|
| logback-classic-1.2.3.jar | sun.reflect.Reflection | REMOVED in Java 11 | HIGH |

**Remediation:** Upgrade to logback-classic 1.2.9+ which includes Java 11 compatibility fixes.

### 2.2 JDK Unsupported APIs (sun.misc.Unsafe)

These dependencies use `sun.misc.Unsafe` which is moved to `jdk.unsupported` module:

| Dependency | Classes Using Unsafe |
|------------|---------------------|
| aspectjweaver-1.9.2.jar | ClassLoaderWeavingAdaptor |
| guava-20.0.jar | Striped64, LittleEndianByteArray, UnsignedBytes, AbstractFuture |
| lombok-1.18.6.jar | AnnotationProcessorHider |
| objenesis-2.6.jar | UnsafeFactoryInstantiator, ClassDefinitionUtils, UnsafeUtils |
| spring-core-5.1.6.RELEASE.jar | UnsafeFactoryInstantiator, DefineClassHelper, UnsafeUtils |

**Risk Level:** MEDIUM  
**Impact:** These will generate warnings but continue to work on Java 11.  
**Remediation:** 
- Upgrade Guava from 20.0 to 31.0+ for better Java 11 support
- Upgrade Lombok from 1.18.6 to 1.18.20+ for Java 11 compatibility
- Spring Boot upgrade will bring updated versions of other dependencies

### 2.3 JDK Compiler Internal APIs

| Dependency | Internal API |
|------------|--------------|
| lombok-1.18.6.jar | com.sun.tools.javac.processing.JavacFiler |
| lombok-1.18.6.jar | com.sun.tools.javac.processing.JavacProcessingEnvironment |
| lombok-1.18.6.jar | com.sun.tools.javac.util.Context |
| lombok-1.18.6.jar | com.sun.tools.javac.util.Options |

**Risk Level:** MEDIUM  
**Remediation:** Upgrade Lombok to 1.18.20+ which has proper Java 11 support.

## 3. Deprecated API Usage in Application Code

### 3.1 jdeprscan Results

**Result:** No deprecated APIs found in application code (`target/classes`).

The application code does not directly use any APIs deprecated for removal in Java 11.

### 3.2 Manual Code Review Findings

#### 3.2.1 WebSecurityConfigurerAdapter Usage

**File:** `src/main/java/com/coding/exercise/bankapp/config/SecurityConfig.java`

```java
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        // ...
    }
}
```

**Status:** Not deprecated in Spring Security 5.1.5 (current version)  
**Note:** This class is deprecated in Spring Security 5.7+ but works fine with the current Spring Boot 2.1.4 version.  
**Risk Level:** LOW (for Java 11 migration)  
**Future Consideration:** If upgrading Spring Boot beyond 2.7.x, this will need to be refactored to component-based security configuration.

#### 3.2.2 Springfox Swagger Configuration

**File:** `src/main/java/com/coding/exercise/bankapp/config/ApplicationConfig.java`

**Status:** Springfox 2.9.2 is compatible with Java 11 but has known issues with Spring Boot 2.6+  
**Risk Level:** LOW (for Java 11 migration with current Spring Boot version)

## 4. Dependency Inventory

### 4.1 Current Dependencies Summary

| Category | Dependency | Version | Java 11 Compatible |
|----------|------------|---------|-------------------|
| Framework | Spring Boot | 2.1.4.RELEASE | Yes |
| ORM | Hibernate | 5.3.9.Final | Yes |
| Database | H2 | 1.4.199 | Yes |
| Logging | Logback | 1.2.3 | Partial (needs upgrade) |
| Build Tool | Lombok | 1.18.6 | Partial (needs upgrade) |
| API Docs | Springfox | 2.9.2 | Yes |
| Utility | Guava | 20.0 | Partial (warnings) |

### 4.2 Java EE/Jakarta EE Dependencies (Transitive)

| Module | Dependency | Version | Source |
|--------|------------|---------|--------|
| JAXB API | javax.xml.bind:jaxb-api | 2.3.1 | spring-boot-starter-data-jpa |
| Activation | javax.activation:javax.activation-api | 1.2.0 | jaxb-api |
| Annotation | javax.annotation:javax.annotation-api | 1.3.2 | spring-boot-starter |
| Transaction | javax.transaction:javax.transaction-api | 1.3 | spring-boot-starter-data-jpa |
| Persistence | javax.persistence:javax.persistence-api | 2.2 | hibernate-core |
| Validation | javax.validation:validation-api | 2.0.1.Final | hibernate-validator |

## 5. Risk Assessment Summary

### 5.1 Risk Matrix

| Risk Category | Level | Count | Action Required |
|---------------|-------|-------|-----------------|
| Removed JDK Modules | NONE | 0 | No |
| Removed Internal APIs | HIGH | 1 | Yes - Upgrade logback |
| Unsupported Internal APIs | MEDIUM | 5 | Recommended - Upgrade dependencies |
| Deprecated Application Code | NONE | 0 | No |

### 5.2 Overall Migration Risk: LOW to MEDIUM

The codebase is well-positioned for Java 11 migration with minimal changes required.

## 6. Remediation Plan

### 6.1 Required Changes (Before Migration)

1. **Upgrade Logback** (HIGH Priority)
   - Current: 1.2.3
   - Target: 1.2.9+ (managed by Spring Boot)
   - Reason: Uses removed `sun.reflect.Reflection` API

### 6.2 Recommended Changes (During Migration)

2. **Upgrade Lombok** (MEDIUM Priority)
   - Current: 1.18.6
   - Target: 1.18.20+
   - Reason: Better Java 11 compiler API support

3. **Consider Spring Boot Upgrade** (MEDIUM Priority)
   - Current: 2.1.4.RELEASE
   - Target: 2.5.x or 2.7.x (for Java 11 LTS support)
   - Benefits: Updated dependencies with better Java 11 support

### 6.3 Optional Changes (Post-Migration)

4. **Upgrade Guava** (LOW Priority)
   - Current: 20.0 (transitive via Springfox)
   - Target: 31.0+
   - Reason: Reduces illegal reflective access warnings

## 7. Build Configuration Changes Required

### 7.1 Maven pom.xml Updates

```xml
<properties>
    <java.version>11</java.version>
    <!-- or use maven.compiler.release for Java 11+ -->
    <maven.compiler.release>11</maven.compiler.release>
</properties>
```

### 7.2 Plugin Updates Recommended

- maven-compiler-plugin: 3.11.0+
- maven-surefire-plugin: 3.2.5+
- maven-failsafe-plugin: 3.2.5+

## 8. Testing Recommendations

1. Run full test suite on Java 11 JDK
2. Monitor for illegal reflective access warnings in logs
3. Verify H2 console access works correctly
4. Test Swagger UI functionality
5. Validate all REST endpoints

## 9. Conclusion

The BankApp codebase is in good condition for Java 11 migration. The application code does not use any deprecated or removed APIs directly. The main concerns are related to third-party dependencies using JDK internal APIs, which can be addressed through dependency upgrades.

**Recommended Migration Approach:**
1. Upgrade Lombok to 1.18.20+
2. Update pom.xml to target Java 11
3. Upgrade maven plugins
4. Run tests and address any issues
5. Consider Spring Boot upgrade for long-term maintainability

## Appendix A: Tool Versions Used

- jdeps: OpenJDK 17.0.x (for --release 11 analysis)
- jdeprscan: OpenJDK 17.0.x (for --release 11 analysis)
- Maven: 3.6.3

## Appendix B: Files Analyzed

```
src/main/java/com/coding/exercise/bankapp/
├── BankingApplication.java
├── config/
│   ├── ApplicationConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AccountController.java
│   └── CustomerController.java
├── domain/
│   ├── AccountInformation.java
│   ├── AddressDetails.java
│   ├── BankInformation.java
│   ├── ContactDetails.java
│   ├── CustomerDetails.java
│   ├── TransactionDetails.java
│   └── TransferDetails.java
├── model/
│   ├── Account.java
│   ├── Address.java
│   ├── BankInfo.java
│   ├── Contact.java
│   ├── Customer.java
│   ├── CustomerAccountXRef.java
│   └── Transaction.java
├── repository/
│   ├── AccountRepository.java
│   ├── CustomerAccountXRefRepository.java
│   ├── CustomerRepository.java
│   └── TransactionRepository.java
└── service/
    ├── BankingService.java
    ├── BankingServiceImpl.java
    └── helper/
        └── BankingServiceHelper.java
```

## Appendix C: Full jdeps Output

```
aspectjweaver-1.9.2.jar -> jdk.unsupported
   org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor -> sun.misc.Unsafe

guava-20.0.jar -> jdk.unsupported
   com.google.common.cache.Striped64 -> sun.misc.Unsafe
   com.google.common.hash.LittleEndianByteArray$UnsafeByteArray -> sun.misc.Unsafe
   com.google.common.primitives.UnsignedBytes$LexicographicalComparatorHolder$UnsafeComparator -> sun.misc.Unsafe
   com.google.common.util.concurrent.AbstractFuture$UnsafeAtomicHelper -> sun.misc.Unsafe

logback-classic-1.2.3.jar -> JDK removed internal API
   ch.qos.logback.classic.spi.PackagingDataCalculator -> sun.reflect.Reflection

lombok-1.18.6.jar -> jdk.compiler, jdk.unsupported
   lombok.javac.apt.Processor -> com.sun.tools.javac.processing.JavacFiler
   lombok.javac.apt.Processor -> com.sun.tools.javac.processing.JavacProcessingEnvironment
   lombok.javac.apt.Processor -> com.sun.tools.javac.util.Context
   lombok.javac.apt.Processor -> com.sun.tools.javac.util.Options
   lombok.launch.AnnotationProcessorHider$AnnotationProcessor -> sun.misc.Unsafe

objenesis-2.6.jar -> jdk.unsupported
   org.objenesis.instantiator.sun.UnsafeFactoryInstantiator -> sun.misc.Unsafe
   org.objenesis.instantiator.util.ClassDefinitionUtils -> sun.misc.Unsafe
   org.objenesis.instantiator.util.UnsafeUtils -> sun.misc.Unsafe

spring-core-5.1.6.RELEASE.jar -> jdk.unsupported
   org.springframework.objenesis.instantiator.sun.UnsafeFactoryInstantiator -> sun.misc.Unsafe
   org.springframework.objenesis.instantiator.util.DefineClassHelper$Java8 -> sun.misc.Unsafe
   org.springframework.objenesis.instantiator.util.UnsafeUtils -> sun.misc.Unsafe
```

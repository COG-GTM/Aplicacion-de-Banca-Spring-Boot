# Baseline: JDK 11 / Spring Boot 2.7.18 (ticket UNT2-1, plan step s1.1)

Captured 2026-09-23 on commit `188b2d8` (default branch) with OpenJDK 11.0.32.1, Maven wrapper 3.8.8.
No code changes were made for this baseline.

## `./mvnw -B clean verify`

| Item | Result |
|------|--------|
| Build | `BUILD SUCCESS` |
| Tests | 1 run, 0 failures, 0 errors, 0 skipped (`BankingApplicationTests` context load, 3.7 s) |
| Total time | 7.6 s (warm `~/.m2`) |
| Plugins | maven-compiler-plugin 3.11.0 (`release` 11), maven-surefire-plugin 3.2.5 |

Environment note: `./mvnw` failed to download its distribution from `repo.maven.apache.org`
(HTTP 429 from this box). Workaround: pre-seed `~/.m2/wrapper/dists/apache-maven-3.8.8/<hash>/`
with the 3.8.8 binary from `archive.apache.org`. Not a repo problem.

## Dependency tree

See [`dependency-tree-jdk11.txt`](dependency-tree-jdk11.txt) (`./mvnw -B dependency:tree`).
Key coordinates that the Boot 4 / Jakarta migration must move:

- Spring Boot 2.7.18, Spring Framework 5.3.31, Spring Security 5.7.11, Spring Data JPA 2.7.18
- Hibernate 5.6.15.Final, `jakarta.persistence-api` 2.2.3 (`javax.*` namespace), H2 2.1.214, HikariCP 4.0.3
- Tomcat 9.0.83, Jackson 2.13.5, Logback 1.2.12, JUnit 5.8.2, Mockito 4.5.1
- springdoc-openapi-ui 1.6.15 (webjars swagger-ui 4.17.1), Lombok 1.18.30
- `org.glassfish.jaxb:jaxb-runtime` 2.3.8 (explicit; javax JAXB)

## Runtime smoke (`java -jar target/bank-app-1.0.0.jar`)

Startup: `Started BankingApplication in 3.416 seconds`, Tomcat on 8989, context path `/bank-api`,
H2 console at `/bank-api/h2-console/` (JDBC URL `jdbc:h2:mem:<uuid>` printed in log, user `sa`, no password).

| Check | Result |
|-------|--------|
| `GET /bank-api/swagger-ui/index.html` | 200 |
| `GET /bank-api/v3/api-docs` | 200 |
| `GET /bank-api/h2-console/` | 200 (login with logged JDBC URL works) |
| `GET /bank-api/actuator/health` | 200 `{"status":"UP"}` |
| `POST /bank-api/customers/add` | 201 `New Customer created successfully.` |
| `GET /bank-api/customers/{customerNumber}` | **500** (see defect below) |
| `GET /bank-api/customers/all` | **500** |
| `POST /bank-api/accounts/add/{customerNumber}` | **500** |
| `PUT /bank-api/accounts/transfer/{customerNumber}` | **500** |
| `GET /bank-api/accounts/{accountNumber}` (unknown) | 404 `Account Number N not found.` |
| `GET /bank-api/accounts/transactions/{accountNumber}` | 200 `[]` |

Script: [`e2e-roundtrip.sh`](e2e-roundtrip.sh) (expected shapes encoded as the phase-4 regression
checks; it currently exits non-zero on the baseline because of the defect below).
Raw run output: [`e2e-roundtrip-jdk11.out`](e2e-roundtrip-jdk11.out).

### Baseline defect: UUID ids are unreadable on H2 2.x with Hibernate 5.6

Every entity uses `@Id @GeneratedValue(strategy = AUTO) private UUID id`. Hibernate 5.6 maps
`UUID` on H2 to `BINARY(255)`; H2 2.x treats `BINARY(n)` as fixed-length and zero-pads the 16-byte
UUID to 255 bytes, so `Customer.contactDetails` (a `@OneToOne` FK) cannot be resolved on read:

```
javax.persistence.EntityNotFoundException: Unable to find com.coding.exercise.bankapp.model.Contact with id 00e2428b-...
```

Verified in the H2 console: `INFORMATION_SCHEMA.COLUMNS` shows `CONTACT_ID BINARY 255`, and the stored
key is `00e2428b882445c78afe756b325607c6` followed by zero padding. Writes succeed (1 row in `CUSTOMER`,
1 in `CONTACT`) but any path that loads a customer (`GET /customers/**`, `POST /accounts/add/**`,
`PUT /accounts/transfer/**`) returns 500.

Consequence for the plan: the customer -> account -> transfer round trip **does not work on the
baseline**, so it cannot be a like-for-like regression check for phase 4. Hibernate 6 (Boot 3/4)
maps `UUID` natively on H2 (`UUID` column type), so the round trip is expected to start passing
after the migration; phase 4 should treat `e2e-roundtrip.sh` exiting 0 as the acceptance signal
and record the fix as a behaviour change, not a regression. Alternative if a green baseline is
wanted first: pin `com.h2database:h2` to 1.4.200 (or add `@Type(type="uuid-char")` on ids) in a
separate ticket, but that is a code change and out of scope for s1.1.

## Request/response shapes (for phase-4 regression checks)

- `POST /customers/add` body `CustomerDetails`:
  `{firstName,lastName,middleName,customerNumber:Long,status,customerAddress:{address1,address2,city,state,zip,country},contactDetails:{emailId,homePhone,workPhone}}`
  -> 201, text body `New Customer created successfully.`
- `GET /customers/{customerNumber}` -> 200 `CustomerDetails` JSON (baseline: 500)
- `POST /accounts/add/{customerNumber}` body `AccountInformation`:
  `{accountNumber:Long,bankInformation:{branchName,branchCode:Integer,routingNumber:Integer,branchAddress:{...}},accountStatus,accountType,accountBalance:Double}`
  -> 201 `New Account created successfully.`
- `GET /accounts/{accountNumber}` -> **302 FOUND** with `AccountInformation` JSON body (not 200; keep as-is), 404 text when missing
- `PUT /accounts/transfer/{customerNumber}` body `{fromAccountNumber,toAccountNumber,transferAmount:Double}`
  -> 200 `Success: Amount transferred for Customer Number {customerNumber}`; 400 `Insufficient Funds.`; 404 `From/To Account Number N not found.`
- `GET /accounts/transactions/{accountNumber}` -> 200 `TransactionDetails[]`
- Security: `spring-boot-starter-security` is on the classpath with `spring.security.user.*` set in
  `application.yml`, yet all endpoints answer anonymously: `SecurityConfig` (`WebSecurityConfigurerAdapter`,
  removed in Security 6) only calls `authorizeRequests()` for `/` and `/h2-console/**` with `permitAll()`, disables CSRF and
  enables frame options for the H2 console; no `anyRequest().authenticated()` is declared. Preserve the no-auth behaviour in phase 3.

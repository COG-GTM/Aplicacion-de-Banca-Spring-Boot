# Banking Application using Java 21, Spring Boot 4, Spring Security and H2 DB

RESTful API to simulate simple banking operations.

## Requirements

* CRUD operations for customers and accounts.
* Support deposits and withdrawals on accounts.
* Internal transfer support (i.e. a customer may transfer funds from one account to another).

## Getting Started

1. Checkout the project from GitHub

   ```bash
   git clone https://github.com/COG-GTM/Aplicacion-de-Banca-Spring-Boot.git
   cd Aplicacion-de-Banca-Spring-Boot
   ```

2. Build and run with the Maven wrapper (no local Maven install needed)

   ```bash
   export JAVA_HOME=/path/to/jdk-21
   ./mvnw -B clean verify
   java -jar target/bank-app-1.0.0.jar
   ```

3. Enable Lombok support on your IDE (see <https://projectlombok.org/setup/eclipse>) and import
   the project as an existing Maven project.

4. Default port for the API is 8989, context path `/bank-api`. No authentication is required for
   any endpoint.

### Prerequisites

* Java 21 (LTS) - OpenJDK 21 or later (`maven-enforcer-plugin` rejects anything below 21)
* Maven 3.9+ - the bundled wrapper (`./mvnw`, Maven 3.9.16) is recommended
* Spring Tool Suite 4, IntelliJ IDEA or a similar IDE

### Maven Dependencies

Spring Boot 4.1.1 (`spring-boot-starter-parent`) manages every version below unless noted.

```text
spring-boot-starter-actuator
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-webmvc          (Boot 4 name of spring-boot-starter-web)
spring-boot-devtools
spring-boot-h2console               (H2 web console, split out of the JPA/JDBC starters in Boot 4)
h2                                  - in-memory database
lombok                              - to reduce boilerplate code
springdoc-openapi-starter-webmvc-ui 3.1.1 - API documentation (OpenAPI 3)
spring-boot-starter-test            (JUnit 6, AssertJ, Mockito)
spring-boot-starter-webmvc-test     (MockMvc)
spring-boot-starter-data-jpa-test
spring-boot-starter-security-test
```

## API Documentation

Please find the REST API documentation (OpenAPI 3) at the URL below
(`/swagger-ui.html` still redirects there):

```text
http://localhost:8989/bank-api/swagger-ui/index.html
http://localhost:8989/bank-api/v3/api-docs
```

### Endpoints

Customer management (`/customers`):

* `GET /customers/all` - list all customers
* `POST /customers/add` - create a customer
* `GET /customers/{customerNumber}` - get customer details
* `PUT /customers/{customerNumber}` - update customer information
* `DELETE /customers/{customerNumber}` - delete a customer and its accounts

Accounts and transactions (`/accounts`):

* `GET /accounts/{accountNumber}` - get account details
* `POST /accounts/add/{customerNumber}` - create an account for a customer
* `PUT /accounts/transfer/{customerNumber}` - transfer funds between two of the customer's accounts
* `GET /accounts/transactions/{accountNumber}` - transaction history of an account

Actuator: `GET /bank-api/actuator/health` (returns `status` plus the `liveness` and `readiness`
groups; `/actuator/health/liveness` and `/actuator/health/readiness` are also available).

## H2 In-Memory Database

The JDBC URL (`jdbc:h2:mem:<uuid>`) is printed in the startup log; user `sa`, empty password.
If you intend to use a custom database name, define the datasource properties in
`application.yml`.

```text
http://localhost:8989/bank-api/h2-console/
```

## Testing the Bank APP Rest Api

1. Run the automated suite: `./mvnw -B test` (context load, MockMvc regression tests for the
   customer -> account -> transfer round trip, and `SecurityConfig` tests).

2. Please use the Swagger URL to perform CRUD operations manually.

3. Browse to `<project-root>/src/test/resources` to find sample requests to add customers and
   accounts, and `docs/baseline/e2e-roundtrip.sh` for a scripted curl round trip against a running
   instance.

## Troubleshooting

| Symptom | Cause | Solution |
| ------- | ----- | -------- |
| Port 8989 already in use | Another app using the port | Change `server.port` or stop the process |
| Enforcer: `not in the allowed range [21,)` | JDK older than 21 | Install JDK 21, set `JAVA_HOME` |
| H2 console not accessible | `spring-boot-h2console` missing | Keep it; console `enabled=true` |
| Swagger UI not loading | Context path | Access via `/bank-api/swagger-ui/index.html` |

## Migration Notes

This application was migrated from Java 8 to Java 11 (LTS), and then from Java 11 / Spring Boot
2.7.18 to Java 21 (LTS) / Spring Boot 4.1.1. See `MIGRATION_NOTES.md` for the changes made in
each step, `docs/spring-boot-compatibility.md` for the dependency-by-dependency analysis and
`docs/baseline/` for the before/after verification evidence.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Authors

* **Shyam Bathina**

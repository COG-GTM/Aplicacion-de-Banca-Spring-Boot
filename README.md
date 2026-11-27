# Banking Application using Java 17, Spring Boot 4, Spring Security and H2 DB

RESTful API to simulate simple banking operations. 

## Requirements

*	CRUD operations for customers and accounts.
*	Support deposits and withdrawals on accounts.
*	Internal transfer support (i.e. a customer may transfer funds from one account to another).


## Getting Started

1. Checkout the project from GitHub

```
git clone https://github.com/sbathina/BankApp

```
2. Enable Lombok support on your IDE

Refer to the following link for instructions:

```
https://projectlombok.org/setup/eclipse

```
3. Open IDE of your choice and Import as existing maven project in your workspace

```
- Import existing maven project
- Run mvn clean install
- If using STS, Run As Spring Boot App

```
4. Default port for the api is 8989


### Prerequisites

* Java 17 (LTS) - OpenJDK 17 or later
* Spring Tool Suite 4 or similar IDE
* [Maven](https://maven.apache.org/) - Dependency Management

### Maven Dependencies

```
spring-boot-starter-actuator
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-webmvc
spring-boot-devtools
h2 - Inmemory database
lombok - to reduce boilerplate code
springdoc-openapi-starter-webmvc-ui - API documentation (OpenAPI 3.1)
spring-boot-starter-test
spring-boot-starter-webmvc-test
spring-boot-starter-security-test

```

## API Documentation

Please find the Rest API documentation (OpenAPI 3.0) in the below url

```
http://localhost:8989/bank-api/swagger-ui.html

```

## H2 In-Memory Database

The application uses an H2 in-memory database. The H2 web console is no longer exposed: it cannot work
with CSRF protection enabled, and an unauthenticated database console should not be reachable.

## CSRF Protection

CSRF protection is enabled. Every response sets an `XSRF-TOKEN` cookie; send its value back in the
`X-XSRF-TOKEN` header (together with the cookie) on POST, PUT and DELETE requests. Swagger UI does this
automatically. With curl:

```
curl -c cookies.txt http://localhost:8989/bank-api/customers/all
curl -b cookies.txt -H "X-XSRF-TOKEN: $(awk '/XSRF-TOKEN/ {print $7}' cookies.txt)" \
     -H "Content-Type: application/json" -d @src/test/resources/AddCustomer_1000.json \
     http://localhost:8989/bank-api/customers/add
```

## Testing the Bank APP Rest Api

1. Please use the Swagger url to perform CRUD operations. 

2. Browse to <project-root>/src/test/resources to find sample requests to add customer and accounts.


## Migration Notes

This application has been migrated from Java 8 to Java 11 (LTS), and later to Java 17 with Spring Boot 4 to remediate Snyk findings. See `MIGRATION_NOTES.md` for detailed information about the migration process and changes made.

## Authors

* **Shyam Bathina**


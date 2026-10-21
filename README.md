# Banking Application using Java 11, Spring Boot, Spring Security and H2 DB

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

* Java 11 (LTS) - OpenJDK 11 or later
* Spring Tool Suite 4 or similar IDE
* [Maven](https://maven.apache.org/) - Dependency Management

### Maven Dependencies

```
spring-boot-starter-actuator
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-web
spring-boot-devtools
h2 - Inmemory database
lombok - to reduce boilerplate code
springdoc-openapi-ui - API documentation (OpenAPI 3.0)
spring-boot-starter-test
spring-security-test

```

## API Documentation

Please find the Rest API documentation (OpenAPI 3.0) in the below url

```
http://localhost:8989/bank-api/swagger-ui.html

```

## H2 In-Memory Database

Make sure to use jdbc:h2:mem:testdb as your jdbc url. If you intend to you use custom database name, please
define datasource properties in application.yml

```
http://localhost:8989/bank-api/h2-console/

```

## Testing the Bank APP Rest Api

1. Please use the Swagger url to perform CRUD operations. 

2. Browse to <project-root>/src/test/resources to find sample requests to add customer and accounts.


## Bulk Account Number Management

Manage many account numbers in one call (default limit: 100 per request, configurable with
`bankapp.bulk.max-batch-size`). Each account number gets its own result, so one bad entry does not
fail the batch. Customer-scoped operations only act on accounts linked to that customer.

| Method | Path | Body | Per-item statuses |
| ------ | ---- | ---- | ----------------- |
| POST | `/accounts/bulk/add/{customerNumber}` | `{"accounts": [AccountInformation, ...]}` | `CREATED`, `DUPLICATE`, `INVALID` |
| POST | `/accounts/bulk/lookup` | `{"accountNumbers": [5000, 5001]}` | `FOUND`, `NOT_FOUND` |
| PUT | `/accounts/bulk/status/{customerNumber}` | `{"accountNumbers": [...], "accountStatus": "Frozen"}` | `UPDATED`, `NOT_FOUND`, `NOT_OWNED` |
| POST | `/accounts/bulk/delete/{customerNumber}` | `{"accountNumbers": [...]}` | `DELETED`, `REJECTED` (non-zero balance), `NOT_FOUND`, `NOT_OWNED` |

New accounts without an `accountBalance` start at `0`; text fields longer than 255 characters are reported as `INVALID`.

Repeated or missing account numbers in a request are reported as `DUPLICATE` / `INVALID`. Responses
look like `{"operation": "ADD", "requested": 3, "succeeded": 2, "failed": 1, "results": [...]}`.
An empty or oversized batch returns `400`; an unknown customer returns `404`. See
`src/test/resources/bulkAddAccounts_Customer1000.json` for a sample request.

## Migration Notes

This application has been migrated from Java 8 to Java 11 (LTS). See `MIGRATION_NOTES.md` for detailed information about the migration process and changes made.

## Authors

* **Shyam Bathina**


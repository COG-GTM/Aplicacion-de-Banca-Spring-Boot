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

## Bulk Customer Update

Use `PUT /bank-api/customers/bulk` to update up to 100 customers in one request. Only non-null fields are changed; omitted fields remain unchanged.

```json
{
  "customers": [
    {
      "customerNumber": 1000,
      "lastName": "Updated",
      "contactDetails": {
        "emailId": "updated@example.com"
      }
    }
  ]
}
```

The response includes one result per requested item, in request order:

```json
{
  "requested": 1,
  "updated": 1,
  "failed": 0,
  "results": [
    {
      "customerNumber": 1000,
      "status": "UPDATED",
      "message": "Customer updated."
    }
  ]
}
```

Result statuses are `UPDATED`, `NOT_FOUND`, `INVALID`, and `DUPLICATE`. Empty batches and batches over 100 items return `400 Bad Request`. Empty nested objects count as no change, and provided string fields over 255 characters are rejected as `INVALID`.

## H2 In-Memory Database

Make sure to use jdbc:h2:mem:testdb as your jdbc url. If you intend to you use custom database name, please
define datasource properties in application.yml

```
http://localhost:8989/bank-api/h2-console/

```

## Testing the Bank APP Rest Api

1. Please use the Swagger url to perform CRUD operations. 

2. Browse to <project-root>/src/test/resources to find sample requests to add customer and accounts.


## Migration Notes

This application has been migrated from Java 8 to Java 11 (LTS). See `MIGRATION_NOTES.md` for detailed information about the migration process and changes made.

## Authors

* **Shyam Bathina**

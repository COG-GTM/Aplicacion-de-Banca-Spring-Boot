# Contributing

## Development Setup

- JDK 21 or later is required (`maven-enforcer-plugin` enforces `[21,)`); set `JAVA_HOME` to it.
- Use the Maven wrapper: `./mvnw -B clean verify` (build + tests), `./mvnw -B test` (tests only),
  `./mvnw -B -DskipTests package` then `java -jar target/bank-app-1.0.0.jar` to run.
- Follow the README "Getting Started" section for IDE and Lombok setup.
- Create branches: `devin/<short-purpose>`.

## Coding Standards

- Target Java 21 / Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7, Hibernate ORM 7,
  Jackson 3, Jakarta EE 11 namespaces - `jakarta.*`, never `javax.*`).
- Format + lint before pushing (`./mvnw -B compile` runs with `-Xlint:all`).
- Write tests for new features (MockMvc tests live in `src/test/java`).

## Pull Requests

- Small, focused changes.
- Include screenshots/logs for docs updates.
- Link related issues.
- CI runs `.github/workflows/ci.yml` (JDK 21 build, tests, verify) and
  `.github/workflows/docs-ci.yml` (markdownlint + link check) on every PR.

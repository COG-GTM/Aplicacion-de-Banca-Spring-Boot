package com.coding.exercise.bankapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

class H2ConsoleSecurityTests {

    private static final String CONSOLE = "/bank-api/h2-console/";

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    class DefaultProfile {

        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        void consoleIsNotRegistered() {
            ResponseEntity<String> response = restTemplate.withBasicAuth("bankapp", "changeit")
                    .getForEntity(CONSOLE, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void apiRemainsReachable() {
            ResponseEntity<String> response = restTemplate.getForEntity("/bank-api/customers/all", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @ActiveProfiles("local")
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    class LocalProfile {

        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        void consoleRequiresAuthentication() {
            ResponseEntity<String> response = restTemplate.getForEntity(CONSOLE, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void consoleRejectsWrongCredentials() {
            ResponseEntity<String> response = restTemplate.withBasicAuth("bankapp", "wrong")
                    .getForEntity(CONSOLE, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void authenticatedUserGetsConsoleWithSameOriginFraming() {
            ResponseEntity<String> response = restTemplate.withBasicAuth("bankapp", "changeit")
                    .getForEntity(CONSOLE, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
            assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        }
    }
}

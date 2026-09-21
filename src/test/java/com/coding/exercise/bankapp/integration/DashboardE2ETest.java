package com.coding.exercise.bankapp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.support.DashboardTestData;

/**
 * End-to-end tests (TC-13 .. TC-16) driving the running application over HTTP
 * and asserting on the rendered dashboard page.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DashboardE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DashboardTestData testData;

    @Value("${spring.security.user.name}")
    private String username;

    @Value("${spring.security.user.password}")
    private String password;

    @BeforeEach
    public void seedDatabase() {
        testData.reset();
    }

    /** TC-13: the dashboard page loads and shows the balance overview. */
    @Test
    public void dashboardPageShowsBalanceOverview() {
        String page = getDashboard();

        assertThat(page).contains("Balance Overview");
        assertThat(page).contains("$58050.50");
        assertThat(page).contains("2 account(s)");
        assertThat(page).contains("$15750.50").contains("$42300.00");
        assertThat(page).contains("Acct #5001").contains("Acct #5002");
        assertThat(page).contains("+$3000.00").contains("-$2500.00");
        assertThat(page).contains("Carlos Rodriguez").contains(">CR<");
    }

    /** TC-14: transactions are listed with the attributes the client-side filter relies on. */
    @Test
    public void transactionsAreListedWithFilterMetadata() {
        String page = getDashboard();

        assertThat(page).contains("Recent Transactions");
        assertThat(page).contains("id=\"txSearch\"").contains("id=\"txTypeFilter\"");
        assertThat(page).contains("data-type=\"DEBIT\"").contains("data-type=\"CREDIT\"");
        assertThat(page).contains("data-description=\"DEBIT - Account 5001\"");
        assertThat(page).contains("Account #5001").contains("Account #5002");
        assertThat(page).contains("+$2500.00").contains("-$2500.00");

        ResponseEntity<String> script = authenticated()
                .getForEntity("/js/dashboard.js", String.class);
        assertThat(script.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(script.getBody()).contains("filterTransactions")
                .contains("data-description")
                .contains("data-type");
    }

    /** TC-15: transferring funds updates the balances shown on the reloaded dashboard. */
    @Test
    public void quickTransferUpdatesDashboardBalances() {
        String beforeTransfer = getDashboard();
        assertThat(beforeTransfer).contains("id=\"transferForm\"");
        assertThat(beforeTransfer).contains("CHECKING #5001 ($15750.50)");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> transfer = authenticated().exchange("/dashboard/transfer", HttpMethod.POST,
                new HttpEntity<>("{\"fromAccountNumber\":5001,\"toAccountNumber\":5002,\"transferAmount\":250.50}",
                        headers),
                String.class);

        assertThat(transfer.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(transfer.getBody()).contains("\"success\":true");

        String afterTransfer = getDashboard();
        assertThat(afterTransfer).contains("$15500.00").contains("$42550.50");
        assertThat(afterTransfer).contains("$58050.50");
    }

    /** TC-16: notifications and the unread badge are rendered for low balance accounts. */
    @Test
    public void notificationsAndBadgeAreRendered() {
        accountRepository.save(DashboardTestData.account(5003L, "CHECKING", 25.00));

        String page = getDashboard();

        assertThat(page).contains("Notifications");
        assertThat(page).contains("Low Balance Alert");
        assertThat(page).contains("Account #5003 balance is below $100.00");
        assertThat(page).contains("Transaction Activity");
        assertThat(page).contains("Quick Transfer Available");
        assertThat(page).contains("notification-item--unread");
        assertThat(page).contains("3 notification(s)");
        assertThat(page).contains("class=\"header__badge\"");
    }

    private String getDashboard() {
        ResponseEntity<String> response = authenticated().getForEntity("/dashboard", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private TestRestTemplate authenticated() {
        return restTemplate.withBasicAuth(username, password);
    }
}

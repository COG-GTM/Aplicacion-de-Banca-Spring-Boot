package com.coding.exercise.bankapp.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.coding.exercise.bankapp.domain.DashboardAccountInfo;
import com.coding.exercise.bankapp.domain.NotificationItem;
import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.TransactionRepository;
import com.coding.exercise.bankapp.support.DashboardTestData;

/**
 * Integration tests (TC-10 .. TC-12) running the full Spring context against
 * the in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DashboardTestData testData;

    @BeforeEach
    public void seedDatabase() {
        testData.reset();
    }

    /** TC-10: the dashboard reads accounts, transactions and customer from the database. */
    @Test
    @WithMockUser
    public void dashboardIsBuiltFromPersistedData() throws Exception {
        MvcResult result = mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attribute("accounts", hasSize(2)))
                .andExpect(model().attribute("transactions", hasSize(3)))
                .andExpect(model().attribute("customerName", "Carlos Rodriguez"))
                .andExpect(model().attribute("totalBalance",
                        DashboardTestData.CHECKING_BALANCE + DashboardTestData.SAVINGS_BALANCE))
                .andExpect(model().attribute("totalIncome", 3000.00))
                .andExpect(model().attribute("totalExpenses", 2500.00))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<DashboardAccountInfo> accounts =
                (List<DashboardAccountInfo>) result.getModelAndView().getModel().get("accounts");
        assertThat(accounts).extracting(DashboardAccountInfo::getAccountNumber)
                .containsExactlyInAnyOrder(DashboardTestData.CHECKING_ACCOUNT, DashboardTestData.SAVINGS_ACCOUNT);
        assertThat(accounts).extracting(DashboardAccountInfo::getAccountType)
                .containsExactlyInAnyOrder("CHECKING", "SAVINGS");
    }

    /** TC-11: a transfer moves funds between two persisted accounts and records transactions. */
    @Test
    @WithMockUser
    public void transferMovesFundsBetweenAccounts() throws Exception {
        mockMvc.perform(post("/dashboard/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountNumber\":5001,\"toAccountNumber\":5002,\"transferAmount\":750.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(balanceOf(DashboardTestData.CHECKING_ACCOUNT))
                .isEqualTo(DashboardTestData.CHECKING_BALANCE - 750.50);
        assertThat(balanceOf(DashboardTestData.SAVINGS_ACCOUNT))
                .isEqualTo(DashboardTestData.SAVINGS_BALANCE + 750.50);

        assertThat(transactionRepository.findByAccountNumber(DashboardTestData.CHECKING_ACCOUNT)
                .orElseThrow(IllegalStateException::new)).hasSize(3);
        assertThat(transactionRepository.findByAccountNumber(DashboardTestData.SAVINGS_ACCOUNT)
                .orElseThrow(IllegalStateException::new)).hasSize(2);
    }

    /** TC-11: a transfer larger than the balance is rejected and leaves balances untouched. */
    @Test
    @WithMockUser
    public void transferIsRejectedWhenFundsAreInsufficient() throws Exception {
        mockMvc.perform(post("/dashboard/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountNumber\":5001,\"toAccountNumber\":5002,\"transferAmount\":999999.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient Funds."));

        assertThat(balanceOf(DashboardTestData.CHECKING_ACCOUNT)).isEqualTo(DashboardTestData.CHECKING_BALANCE);
        assertThat(balanceOf(DashboardTestData.SAVINGS_ACCOUNT)).isEqualTo(DashboardTestData.SAVINGS_BALANCE);
    }

    /** TC-12: notifications are generated from the persisted account and transaction state. */
    @Test
    @WithMockUser
    public void notificationsAreGeneratedFromPersistedState() throws Exception {
        accountRepository.save(DashboardTestData.account(5003L, "CHECKING", 25.00));

        MvcResult result = mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<NotificationItem> notifications =
                (List<NotificationItem>) result.getModelAndView().getModel().get("notifications");
        assertThat(notifications).extracting(NotificationItem::getTitle)
                .containsExactly("Low Balance Alert", "Transaction Activity", "Quick Transfer Available");
        assertThat(notifications.get(0).getMessage()).contains("5003");
        assertThat(result.getModelAndView().getModel().get("notificationCount")).isEqualTo(1L);
    }

    private Double balanceOf(Long accountNumber) {
        Optional<Account> account = accountRepository.findByAccountNumber(accountNumber);
        return account.orElseThrow(IllegalStateException::new).getAccountBalance();
    }
}

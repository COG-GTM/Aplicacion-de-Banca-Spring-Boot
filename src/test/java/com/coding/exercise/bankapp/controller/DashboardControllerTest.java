package com.coding.exercise.bankapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.coding.exercise.bankapp.domain.DashboardAccountInfo;
import com.coding.exercise.bankapp.domain.NotificationItem;
import com.coding.exercise.bankapp.model.Transaction;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerAccountXRefRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;
import com.coding.exercise.bankapp.repository.TransactionRepository;
import com.coding.exercise.bankapp.service.BankingServiceImpl;
import com.coding.exercise.bankapp.service.helper.BankingServiceHelper;

/**
 * Unit tests (TC-01 .. TC-05) covering the DashboardController helpers through
 * the model it exposes to the view, plus the TransferResponse payload.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DashboardControllerTest {

    @Mock
    private BankingServiceImpl bankingService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerAccountXRefRepository custAccXRefRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Spy
    private BankingServiceHelper bankingServiceHelper = new BankingServiceHelper();

    @InjectMocks
    private DashboardController dashboardController;

    private Model renderDashboard() {
        Model model = new ExtendedModelMap();
        dashboardController.dashboard(model);
        return model;
    }

    private void givenAccounts(Object[]... rows) {
        when(accountRepository.findAllAccountSummaries()).thenReturn(Arrays.asList(rows));
    }

    private void givenCustomerNames(Object[]... rows) {
        when(customerRepository.findAllCustomerNames()).thenReturn(Arrays.asList(rows));
    }

    private void givenNoTransactions() {
        when(transactionRepository.findByAccountNumber(any()))
                .thenReturn(Optional.of(Collections.emptyList()));
    }

    /** TC-01: initials are derived from the first and last name, uppercased. */
    @Test
    public void initialsAreDerivedFromFirstAndLastName() {
        givenAccounts();
        givenNoTransactions();
        givenCustomerNames(new Object[] { "carlos", "rodriguez", 1001L });

        assertThat(renderDashboard().getAttribute("customerInitials")).isEqualTo("CR");
    }

    /** TC-01: a single-word name yields one initial, a blank name falls back to "U". */
    @Test
    public void initialsFallBackForSingleWordAndBlankNames() {
        givenAccounts();
        givenNoTransactions();
        givenCustomerNames(new Object[] { "Carlos", "", 1001L });
        assertThat(renderDashboard().getAttribute("customerInitials")).isEqualTo("C");

        givenCustomerNames(new Object[] { "", "", 1001L });
        assertThat(renderDashboard().getAttribute("customerInitials")).isEqualTo("U");
    }

    /** TC-02: the first customer's full name is displayed. */
    @Test
    public void customerNameIsBuiltFromFirstAndLastName() {
        givenAccounts();
        givenNoTransactions();
        givenCustomerNames(
                new Object[] { "Carlos", "Rodriguez", 1001L },
                new Object[] { "Maria", "Lopez", 1002L });

        Model model = renderDashboard();

        assertThat(model.getAttribute("customerName")).isEqualTo("Carlos Rodriguez");
        assertThat(model.getAttribute("customerInitials")).isEqualTo("CR");
    }

    /** TC-02: with no customers the dashboard shows a generic name. */
    @Test
    public void customerNameFallsBackWhenNoCustomerExists() {
        givenAccounts();
        givenNoTransactions();
        givenCustomerNames();

        assertThat(renderDashboard().getAttribute("customerName")).isEqualTo("Banking User");
    }

    /** TC-03: notifications are generated for low balances, activity and transfer availability. */
    @Test
    public void notificationsAreBuiltFromAccountsAndTransactions() {
        givenAccounts(
                new Object[] { 5001L, "CHECKING", "Active", 50.0 },
                new Object[] { 5002L, "SAVINGS", "Active", 42300.0 });
        givenCustomerNames(new Object[] { "Carlos", "Rodriguez", 1001L });
        when(transactionRepository.findByAccountNumber(5001L))
                .thenReturn(Optional.of(Collections.singletonList(transaction(5001L, "DEBIT", 25.0))));
        when(transactionRepository.findByAccountNumber(5002L))
                .thenReturn(Optional.of(Collections.emptyList()));

        Model model = renderDashboard();

        List<NotificationItem> notifications = notifications(model);
        assertThat(notifications).extracting(NotificationItem::getTitle)
                .containsExactly("Low Balance Alert", "Transaction Activity", "Quick Transfer Available");
        assertThat(notifications.get(0).getType()).isEqualTo("warning");
        assertThat(notifications.get(0).getMessage()).contains("5001");
        assertThat(notifications.get(0).isRead()).isFalse();
        assertThat(model.getAttribute("notificationCount")).isEqualTo(1L);
    }

    /** TC-03: an empty bank shows the welcome notification only. */
    @Test
    public void welcomeNotificationIsShownWhenThereAreNoAccounts() {
        givenAccounts();
        givenNoTransactions();
        givenCustomerNames();

        Model model = renderDashboard();

        assertThat(notifications(model)).extracting(NotificationItem::getTitle)
                .containsExactly("Welcome to BBVA Net Cash");
        assertThat(model.getAttribute("notificationCount")).isEqualTo(1L);
    }

    /** TC-04: account summary rows are mapped onto DashboardAccountInfo and totalled. */
    @Test
    public void accountSummaryRowsAreMappedAndTotalled() {
        givenAccounts(
                new Object[] { 5001L, "CHECKING", "Active", 15750.50 },
                new Object[] { 5002L, "SAVINGS", "Active", null });
        givenCustomerNames(new Object[] { "Carlos", "Rodriguez", 1001L });
        givenNoTransactions();

        Model model = renderDashboard();

        @SuppressWarnings("unchecked")
        List<DashboardAccountInfo> accounts = (List<DashboardAccountInfo>) model.getAttribute("accounts");
        assertThat(accounts).hasSize(2);
        assertThat(accounts.get(0).getAccountNumber()).isEqualTo(5001L);
        assertThat(accounts.get(0).getAccountType()).isEqualTo("CHECKING");
        assertThat(accounts.get(0).getAccountStatus()).isEqualTo("Active");
        assertThat(accounts.get(0).getAccountBalance()).isEqualTo(15750.50);
        assertThat(accounts.get(1).getAccountBalance()).isEqualTo(0.0);
        assertThat(model.getAttribute("totalAccounts")).isEqualTo(2);
        assertThat(model.getAttribute("totalBalance")).isEqualTo(15750.50);
    }

    /** TC-04: income and expense totals split credits from debits. */
    @Test
    public void incomeAndExpenseTotalsSplitCreditsFromDebits() {
        givenAccounts(new Object[] { 5001L, "CHECKING", "Active", 1000.0 });
        givenCustomerNames(new Object[] { "Carlos", "Rodriguez", 1001L });
        when(transactionRepository.findByAccountNumber(5001L)).thenReturn(Optional.of(Arrays.asList(
                transaction(5001L, "CREDIT", 500.0),
                transaction(5001L, "DEBIT", 200.0),
                transaction(5001L, "DEBIT", 100.0))));

        Model model = renderDashboard();

        assertThat(model.getAttribute("totalIncome")).isEqualTo(500.0);
        assertThat(model.getAttribute("totalExpenses")).isEqualTo(300.0);
    }

    /** TC-05: the TransferResponse payload returned to the browser. */
    @Test
    public void transferResponseExposesSuccessAndMessage() {
        DashboardController.TransferResponse success =
                new DashboardController.TransferResponse(true, "Transfer processed.");
        DashboardController.TransferResponse failure =
                new DashboardController.TransferResponse(false, "Insufficient Funds.");

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getMessage()).isEqualTo("Transfer processed.");
        assertThat(failure.isSuccess()).isFalse();
        assertThat(failure.getMessage()).isEqualTo("Insufficient Funds.");
    }

    @SuppressWarnings("unchecked")
    private List<NotificationItem> notifications(Model model) {
        Object attribute = model.getAttribute("notifications");
        return attribute == null ? new ArrayList<>() : (List<NotificationItem>) attribute;
    }

    private Transaction transaction(Long accountNumber, String type, Double amount) {
        return Transaction.builder()
                .accountNumber(accountNumber)
                .txDateTime(new Date())
                .txType(type)
                .txAmount(amount)
                .build();
    }
}

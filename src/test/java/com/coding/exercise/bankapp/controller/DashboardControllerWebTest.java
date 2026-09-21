package com.coding.exercise.bankapp.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.coding.exercise.bankapp.model.CustomerAccountXRef;
import com.coding.exercise.bankapp.model.Transaction;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerAccountXRefRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;
import com.coding.exercise.bankapp.repository.TransactionRepository;
import com.coding.exercise.bankapp.service.BankingServiceImpl;
import com.coding.exercise.bankapp.service.helper.BankingServiceHelper;

/**
 * Component tests (TC-06 .. TC-09) exercising the dashboard endpoints through
 * the web layer with mocked persistence and the real security configuration.
 */
@WebMvcTest(DashboardController.class)
public class DashboardControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BankingServiceImpl bankingService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private CustomerAccountXRefRepository custAccXRefRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    @SpyBean
    private BankingServiceHelper bankingServiceHelper;

    @BeforeEach
    public void setUp() {
        when(accountRepository.findAllAccountSummaries()).thenReturn(Arrays.asList(
                new Object[] { 5001L, "CHECKING", "Active", 15750.50 },
                new Object[] { 5002L, "SAVINGS", "Active", 42300.00 }));
        when(customerRepository.findAllCustomerNames())
                .thenReturn(Collections.singletonList(new Object[] { "Carlos", "Rodriguez", 1001L }));
        when(transactionRepository.findByAccountNumber(5001L))
                .thenReturn(Optional.of(Collections.singletonList(Transaction.builder()
                        .accountNumber(5001L).txDateTime(new Date()).txType("DEBIT").txAmount(2500.00).build())));
        when(transactionRepository.findByAccountNumber(5002L))
                .thenReturn(Optional.of(Collections.singletonList(Transaction.builder()
                        .accountNumber(5002L).txDateTime(new Date()).txType("CREDIT").txAmount(2500.00).build())));
    }

    /** TC-06: GET /dashboard renders the dashboard view with the expected model. */
    @Test
    @WithMockUser
    public void dashboardRendersModel() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attribute("accounts", hasSize(2)))
                .andExpect(model().attribute("transactions", hasSize(2)))
                .andExpect(model().attribute("totalBalance", 58050.50))
                .andExpect(model().attribute("totalAccounts", 2))
                .andExpect(model().attribute("totalIncome", 2500.00))
                .andExpect(model().attribute("totalExpenses", 2500.00))
                .andExpect(model().attribute("customerName", "Carlos Rodriguez"))
                .andExpect(model().attribute("customerInitials", "CR"))
                .andExpect(model().attributeExists("notifications", "notificationCount"));
    }

    /** TC-07: a successful transfer is reported back as success. */
    @Test
    @WithMockUser
    public void transferReturnsSuccess() throws Exception {
        when(custAccXRefRepository.findByAccountNumber(5001L)).thenReturn(Optional.of(
                CustomerAccountXRef.builder().accountNumber(5001L).customerNumber(1001L).build()));
        when(bankingService.transferDetails(any(), eq(1001L))).thenReturn(
                ResponseEntity.status(HttpStatus.OK).body("Success: Amount transferred for Customer Number 1001"));

        mockMvc.perform(post("/dashboard/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountNumber\":5001,\"toAccountNumber\":5002,\"transferAmount\":100.0}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success: Amount transferred for Customer Number 1001"));
    }

    /** TC-08: a rejected transfer and an unknown account both report failure. */
    @Test
    @WithMockUser
    public void transferReturnsFailure() throws Exception {
        when(custAccXRefRepository.findByAccountNumber(5001L)).thenReturn(Optional.of(
                CustomerAccountXRef.builder().accountNumber(5001L).customerNumber(1001L).build()));
        when(bankingService.transferDetails(any(), eq(1001L))).thenReturn(
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient Funds."));

        mockMvc.perform(post("/dashboard/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountNumber\":5001,\"toAccountNumber\":5002,\"transferAmount\":999999.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient Funds."));

        when(custAccXRefRepository.findByAccountNumber(9999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/dashboard/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountNumber\":9999,\"toAccountNumber\":5002,\"transferAmount\":100.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account owner not found."));
    }

    /** TC-09: dashboard endpoints require an authenticated user. */
    @Test
    public void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/dashboard/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountNumber\":5001,\"toAccountNumber\":5002,\"transferAmount\":100.0}"))
                .andExpect(status().isUnauthorized());
    }
}

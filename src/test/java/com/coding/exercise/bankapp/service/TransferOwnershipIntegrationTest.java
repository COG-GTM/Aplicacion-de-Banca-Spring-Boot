package com.coding.exercise.bankapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerAccountXRefRepository;
import com.coding.exercise.bankapp.repository.TransactionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransferOwnershipIntegrationTest {

	private static final long ALICE = 1001L;
	private static final long MALLORY = 2001L;
	private static final long ALICE_CHECKING = 3000L;
	private static final long ALICE_SAVINGS = 3001L;
	private static final long MALLORY_ACCOUNT = 9000L;
	private static final String ADDRESS = "{\"address1\":\"1 Main St\",\"city\":\"Austin\",\"state\":\"TX\",\"zip\":\"78701\",\"country\":\"US\"}";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	private TransactionRepository transactionRepository;
	@Autowired
	private CustomerAccountXRefRepository custAccXRefRepository;

	@BeforeEach
	void setUp() throws Exception {
		createCustomer(ALICE, status().isCreated());
		createCustomer(MALLORY, status().isCreated());
		createAccount(ALICE, ALICE_CHECKING, 4352.0, status().isCreated());
		createAccount(ALICE, ALICE_SAVINGS, 100.0, status().isCreated());
		createAccount(MALLORY, MALLORY_ACCOUNT, 0.0, status().isCreated());
	}

	@Test
	void rejectsTransferFromAccountOwnedByAnotherCustomer() throws Exception {
		transfer(MALLORY, ALICE_CHECKING, MALLORY_ACCOUNT, 4352.0).andExpect(status().isForbidden());

		assertBalancesUnchanged();
	}

	@Test
	void rejectsTransferUsingVictimCustomerNumberIntoForeignAccount() throws Exception {
		transfer(ALICE, ALICE_CHECKING, MALLORY_ACCOUNT, 4352.0).andExpect(status().isForbidden());

		assertBalancesUnchanged();
	}

	@Test
	void rejectsTransferWithUnknownAccountWithoutDisclosingExistence() throws Exception {
		transfer(MALLORY, 777777L, MALLORY_ACCOUNT, 1.0).andExpect(status().isForbidden());
		transfer(MALLORY, ALICE_CHECKING, MALLORY_ACCOUNT, 1.0).andExpect(status().isForbidden());

		assertBalancesUnchanged();
	}

	@Test
	void allowsTransferBetweenCustomersOwnAccounts() throws Exception {
		transfer(ALICE, ALICE_CHECKING, ALICE_SAVINGS, 352.0).andExpect(status().isOk());

		assertThat(balance(ALICE_CHECKING)).isEqualTo(4000.0);
		assertThat(balance(ALICE_SAVINGS)).isEqualTo(452.0);
		assertThat(transactionRepository.count()).isEqualTo(2);
	}

	@Test
	void rejectsRegisteringExistingAccountNumberUnderAnotherCustomer() throws Exception {
		createAccount(MALLORY, ALICE_CHECKING, 0.0, status().isConflict());

		assertThat(custAccXRefRepository.findByCustomerNumberAndAccountNumber(MALLORY, ALICE_CHECKING)).isEmpty();
		transfer(MALLORY, ALICE_CHECKING, MALLORY_ACCOUNT, 4352.0).andExpect(status().isForbidden());
		assertBalancesUnchanged();
	}

	@Test
	void rejectsDuplicateCustomerNumber() throws Exception {
		createCustomer(ALICE, status().isConflict());
	}

	@Test
	void recreatedCustomerDoesNotInheritDeletedCustomersAccounts() throws Exception {
		mockMvc.perform(delete("/customers/" + ALICE)).andExpect(status().isOk());
		createCustomer(ALICE, status().isCreated());

		assertThat(custAccXRefRepository.findByCustomerNumberAndAccountNumber(ALICE, ALICE_CHECKING)).isEmpty();
		transfer(ALICE, ALICE_CHECKING, ALICE_SAVINGS, 1.0).andExpect(status().isForbidden());
		assertBalancesUnchanged();
	}

	private void assertBalancesUnchanged() {
		assertThat(balance(ALICE_CHECKING)).isEqualTo(4352.0);
		assertThat(balance(ALICE_SAVINGS)).isEqualTo(100.0);
		assertThat(balance(MALLORY_ACCOUNT)).isEqualTo(0.0);
		assertThat(transactionRepository.count()).isZero();
	}

	private Double balance(long accountNumber) {
		return accountRepository.findByAccountNumber(accountNumber).orElseThrow().getAccountBalance();
	}

	private void createCustomer(long customerNumber, ResultMatcher expected) throws Exception {
		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"F\",\"lastName\":\"L\",\"customerNumber\":" + customerNumber + ",\"status\":\"Active\",\"customerAddress\":" + ADDRESS
						+ ",\"contactDetails\":{\"emailId\":\"c@example.com\"}}"))
				.andExpect(expected);
	}

	private void createAccount(long customerNumber, long accountNumber, double balance, ResultMatcher expected) throws Exception {
		mockMvc.perform(post("/accounts/add/" + customerNumber).contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountNumber\":" + accountNumber + ",\"accountStatus\":\"Active\",\"accountType\":\"Checking\",\"accountBalance\":" + balance
						+ ",\"bankInformation\":{\"branchName\":\"Main\",\"branchCode\":1,\"routingNumber\":1,\"branchAddress\":" + ADDRESS + "}}"))
				.andExpect(expected);
	}

	private ResultActions transfer(long customerNumber, long from, long to, double amount) throws Exception {
		return mockMvc.perform(put("/accounts/transfer/" + customerNumber).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\":" + from + ",\"toAccountNumber\":" + to + ",\"transferAmount\":" + amount + "}"));
	}
}

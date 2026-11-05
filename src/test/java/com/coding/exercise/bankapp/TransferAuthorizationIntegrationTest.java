package com.coding.exercise.bankapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.model.Customer;
import com.coding.exercise.bankapp.model.CustomerAccountXRef;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerAccountXRefRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TransferAuthorizationIntegrationTest {

	private static final long ALICE = 1001L;
	private static final long MALLORY = 1002L;
	private static final long ALICE_CHECKING = 3000L;
	private static final long MALLORY_ACCOUNT = 9000L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private CustomerAccountXRefRepository custAccXRefRepository;

	@BeforeEach
	void setUp() {
		createCustomer(ALICE, "Alice");
		createCustomer(MALLORY, "Mallory");
		createAccount(ALICE, ALICE_CHECKING, 4352.0);
		createAccount(MALLORY, MALLORY_ACCOUNT, 10.0);
	}

	@Test
	void transferFromAnotherCustomersAccountIsRejected() throws Exception {
		transfer(MALLORY, ALICE_CHECKING, MALLORY_ACCOUNT, 4352.0).andExpect(status().isForbidden());

		assertBalance(ALICE_CHECKING, 4352.0);
		assertBalance(MALLORY_ACCOUNT, 10.0);
	}

	@Test
	void transferUsingVictimCustomerNumberToForeignAccountIsRejected() throws Exception {
		transfer(ALICE, ALICE_CHECKING, MALLORY_ACCOUNT, 4352.0).andExpect(status().isForbidden());

		assertBalance(ALICE_CHECKING, 4352.0);
		assertBalance(MALLORY_ACCOUNT, 10.0);
	}

	@Test
	void negativeTransferAmountIsRejected() throws Exception {
		transfer(MALLORY, MALLORY_ACCOUNT, ALICE_CHECKING, -4352.0).andExpect(status().isBadRequest());

		assertBalance(ALICE_CHECKING, 4352.0);
		assertBalance(MALLORY_ACCOUNT, 10.0);
	}

	@Test
	void duplicateAccountNumberCannotBeClaimedByAnotherCustomer() throws Exception {
		mockMvc.perform(post("/accounts/add/" + MALLORY).contentType(MediaType.APPLICATION_JSON)
				.content(accountJson(ALICE_CHECKING, 0.0))).andExpect(status().isConflict());

		transfer(MALLORY, ALICE_CHECKING, MALLORY_ACCOUNT, 4352.0).andExpect(status().isForbidden());
		assertBalance(ALICE_CHECKING, 4352.0);
	}

	private void createCustomer(long customerNumber, String firstName) {
		customerRepository.save(Customer.builder().customerNumber(customerNumber).firstName(firstName).status("Active").build());
	}

	private void createAccount(long customerNumber, long accountNumber, double balance) {
		accountRepository.save(Account.builder().accountNumber(accountNumber).accountBalance(balance)
				.accountStatus("Active").accountType("Checking").build());
		custAccXRefRepository.save(CustomerAccountXRef.builder().customerNumber(customerNumber)
				.accountNumber(accountNumber).build());
	}

	private String accountJson(long accountNumber, double balance) {
		return "{\"accountNumber\":" + accountNumber + ",\"accountStatus\":\"Active\",\"accountType\":\"Checking\","
				+ "\"accountBalance\":" + balance + ",\"bankInformation\":{\"branchName\":\"Main\",\"branchAddress\":{\"address1\":\"1 Bank St\",\"city\":\"Town\"}}}";
	}

	private org.springframework.test.web.servlet.ResultActions transfer(long customerNumber, long from, long to,
			double amount) throws Exception {
		String body = "{\"fromAccountNumber\":" + from + ",\"toAccountNumber\":" + to + ",\"transferAmount\":" + amount + "}";
		return mockMvc.perform(put("/accounts/transfer/" + customerNumber).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private void assertBalance(long accountNumber, double expected) {
		assertEquals(expected, accountRepository.findByAccountNumber(accountNumber).get().getAccountBalance());
	}
}

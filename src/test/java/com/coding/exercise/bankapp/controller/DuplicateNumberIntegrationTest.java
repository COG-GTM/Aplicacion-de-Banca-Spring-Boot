package com.coding.exercise.bankapp.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.model.Customer;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DuplicateNumberIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private AccountRepository accountRepository;

	private static final String ADDRESS_JSON = "\"customerAddress\":{\"address1\":\"1 Main St\",\"city\":\"Chicago\","
			+ "\"state\":\"IL\",\"zip\":\"60076\",\"country\":\"USA\"}";

	private static final String ADDRESS_AND_CONTACT = ADDRESS_JSON
			+ ",\"contactDetails\":{\"emailId\":\"a@test.com\",\"homePhone\":\"1\",\"workPhone\":\"2\"}";

	private static String customerJson(long customerNumber, String firstName) {
		return "{\"customerNumber\":" + customerNumber + ",\"firstName\":\"" + firstName
				+ "\",\"lastName\":\"Test\",\"status\":\"Active\"," + ADDRESS_AND_CONTACT + "}";
	}

	private static String accountJson(long accountNumber, double balance) {
		return "{\"accountNumber\":" + accountNumber + ",\"accountBalance\":" + balance
				+ ",\"accountStatus\":\"Active\",\"accountType\":\"Checking\",\"bankInformation\":{"
				+ "\"branchName\":\"Main\",\"branchCode\":1,\"routingNumber\":1," + ADDRESS_JSON.replace("customerAddress", "branchAddress") + "}}";
	}

	@Test
	public void duplicateCustomerNumberIsRejectedAndOriginalStillReadable() throws Exception {
		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON).content(customerJson(1001, "Alice")))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON).content(customerJson(1001, "Mallory")))
				.andExpect(status().isConflict());

		mockMvc.perform(get("/customers/1001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Alice"));
	}

	@Test
	public void duplicateAccountNumberIsRejectedAndOriginalStillReadable() throws Exception {
		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON).content(customerJson(1001, "Alice")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON).content(customerJson(1002, "Mallory")))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/accounts/add/1001").contentType(MediaType.APPLICATION_JSON).content(accountJson(3000, 100.0)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/accounts/add/1002").contentType(MediaType.APPLICATION_JSON).content(accountJson(3000, 0.0)))
				.andExpect(status().isConflict());

		mockMvc.perform(get("/accounts/3000"))
				.andExpect(status().isFound())
				.andExpect(jsonPath("$.accountBalance").value(100.0));
	}

	@Test
	public void databaseRejectsDuplicateCustomerNumber() {
		customerRepository.save(Customer.builder().customerNumber(2001L).firstName("A").build());
		assertThrows(DataIntegrityViolationException.class,
				() -> customerRepository.save(Customer.builder().customerNumber(2001L).firstName("B").build()));
	}

	@Test
	public void databaseRejectsDuplicateAccountNumber() {
		accountRepository.save(Account.builder().accountNumber(4001L).accountBalance(1.0).build());
		assertThrows(DataIntegrityViolationException.class,
				() -> accountRepository.save(Account.builder().accountNumber(4001L).accountBalance(2.0).build()));
	}
}

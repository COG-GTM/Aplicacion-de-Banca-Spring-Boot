package com.coding.exercise.bankapp;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BankingApiRegressionTest {

	private static final Long CUSTOMER_NUMBER = 1001L;
	private static final Long ACCOUNT_NUMBER_1 = 5001L;
	private static final Long ACCOUNT_NUMBER_2 = 5002L;

	private static final String CUSTOMER_JSON = "{"
			+ "\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"middleName\":\"K\","
			+ "\"customerNumber\":" + CUSTOMER_NUMBER + ",\"status\":\"Active\","
			+ "\"customerAddress\":{\"address1\":\"1 Main St\",\"address2\":\"\",\"city\":\"London\","
			+ "\"state\":\"LDN\",\"zip\":\"E1\",\"country\":\"UK\"},"
			+ "\"contactDetails\":{\"emailId\":\"ada@example.com\",\"homePhone\":\"111\",\"workPhone\":\"222\"}"
			+ "}";

	@Autowired
	private MockMvc mockMvc;

	private static String accountJson(Long accountNumber, double balance) {
		return "{"
				+ "\"accountNumber\":" + accountNumber + ","
				+ "\"bankInformation\":{\"branchName\":\"Main\",\"branchCode\":10,\"routingNumber\":123456,"
				+ "\"branchAddress\":{\"address1\":\"2 Bank St\",\"address2\":\"\",\"city\":\"London\","
				+ "\"state\":\"LDN\",\"zip\":\"E2\",\"country\":\"UK\"}},"
				+ "\"accountStatus\":\"Active\",\"accountType\":\"Savings\",\"accountBalance\":" + balance
				+ "}";
	}

	@Test
	@Order(1)
	void createCustomerReturns201() throws Exception {
		mockMvc.perform(post("/customers/add")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CUSTOMER_JSON))
				.andExpect(status().isCreated())
				.andExpect(content().string("New Customer created successfully."));
	}

	@Test
	@Order(2)
	void getCustomerByNumberReturns200WithDetails() throws Exception {
		mockMvc.perform(get("/customers/{n}", CUSTOMER_NUMBER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerNumber").value(CUSTOMER_NUMBER))
				.andExpect(jsonPath("$.firstName").value("Ada"))
				.andExpect(jsonPath("$.lastName").value("Lovelace"))
				.andExpect(jsonPath("$.customerAddress.city").value("London"))
				.andExpect(jsonPath("$.contactDetails.emailId").value("ada@example.com"));
	}

	@Test
	@Order(3)
	void listAllCustomersReturns200Array() throws Exception {
		mockMvc.perform(get("/customers/all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[?(@.customerNumber==" + CUSTOMER_NUMBER + ")]").exists());
	}

	@Test
	@Order(4)
	void createTwoAccountsReturn201() throws Exception {
		mockMvc.perform(post("/accounts/add/{customerNumber}", CUSTOMER_NUMBER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(accountJson(ACCOUNT_NUMBER_1, 1000.0)))
				.andExpect(status().isCreated())
				.andExpect(content().string("New Account created successfully."));

		mockMvc.perform(post("/accounts/add/{customerNumber}", CUSTOMER_NUMBER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(accountJson(ACCOUNT_NUMBER_2, 100.0)))
				.andExpect(status().isCreated())
				.andExpect(content().string("New Account created successfully."));
	}

	@Test
	@Order(5)
	void getAccountReturns302WithJsonBody() throws Exception {
		mockMvc.perform(get("/accounts/{n}", ACCOUNT_NUMBER_1))
				.andExpect(status().isFound())
				.andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER_1))
				.andExpect(jsonPath("$.accountBalance").value(1000.0));
	}

	@Test
	@Order(6)
	void transferUpdatesBalances() throws Exception {
		mockMvc.perform(put("/accounts/transfer/{customerNumber}", CUSTOMER_NUMBER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\":" + ACCOUNT_NUMBER_1
						+ ",\"toAccountNumber\":" + ACCOUNT_NUMBER_2
						+ ",\"transferAmount\":250.0}"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Success: Amount transferred")));

		mockMvc.perform(get("/accounts/{n}", ACCOUNT_NUMBER_1))
				.andExpect(status().isFound())
				.andExpect(jsonPath("$.accountBalance").value(750.0));

		mockMvc.perform(get("/accounts/{n}", ACCOUNT_NUMBER_2))
				.andExpect(status().isFound())
				.andExpect(jsonPath("$.accountBalance").value(350.0));
	}

	@Test
	@Order(7)
	void listTransactionsReturns200() throws Exception {
		mockMvc.perform(get("/accounts/transactions/{n}", ACCOUNT_NUMBER_1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].txType").value("DEBIT"))
				.andExpect(jsonPath("$[0].txAmount").value(250.0))
				.andExpect(jsonPath("$[0].accountNumber").value(ACCOUNT_NUMBER_1));
	}

	@Test
	@Order(8)
	void insufficientFundsReturns400() throws Exception {
		mockMvc.perform(put("/accounts/transfer/{customerNumber}", CUSTOMER_NUMBER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\":" + ACCOUNT_NUMBER_2
						+ ",\"toAccountNumber\":" + ACCOUNT_NUMBER_1
						+ ",\"transferAmount\":99999.0}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Insufficient Funds."));
	}
}

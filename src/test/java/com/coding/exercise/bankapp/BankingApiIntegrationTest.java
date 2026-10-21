package com.coding.exercise.bankapp;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BankingApiIntegrationTest {

	private static final String CUSTOMER_WITH_DETAILS = "{\"firstName\":\"Ana\",\"lastName\":\"Lopez\","
			+ "\"customerNumber\":1001,\"status\":\"ACTIVE\","
			+ "\"customerAddress\":{\"address1\":\"1 Main\",\"city\":\"Madrid\",\"state\":\"MD\","
			+ "\"zip\":\"28001\",\"country\":\"ES\"},"
			+ "\"contactDetails\":{\"emailId\":\"ana@example.com\",\"homePhone\":\"1\",\"workPhone\":\"2\"}}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullCustomerAccountAndTransferFlow() throws Exception {
		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON).content(CUSTOMER_WITH_DETAILS))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/customers/1001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Ana"));

		mockMvc.perform(get("/customers/all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		addAccount(5001, 1000);
		addAccount(5002, 50);

		mockMvc.perform(get("/accounts/5001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountBalance").value(1000.0));

		mockMvc.perform(put("/accounts/transfer/1001").contentType(MediaType.APPLICATION_JSON)
				.content(transferJson(5001, 5002, "100")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/accounts/5001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountBalance").value(900.0));
		mockMvc.perform(get("/accounts/5002"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountBalance").value(150.0));

		mockMvc.perform(get("/accounts/transactions/5001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].txType").value("DEBIT"))
				.andExpect(jsonPath("$[0].txAmount").value(100.0));
		mockMvc.perform(get("/accounts/transactions/5002"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].txType").value("CREDIT"));

		String updatedCustomer = CUSTOMER_WITH_DETAILS.replace("\"lastName\":\"Lopez\"", "\"lastName\":\"Garcia\"");
		mockMvc.perform(put("/customers/1001").contentType(MediaType.APPLICATION_JSON).content(updatedCustomer))
				.andExpect(status().isOk());
		mockMvc.perform(get("/customers/1001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lastName").value("Garcia"));

		mockMvc.perform(delete("/customers/1001")).andExpect(status().isOk());
		mockMvc.perform(get("/customers/1001"))
				.andExpect(status().isNotFound())
				.andExpect(content().string("Customer Number 1001 not found."));
	}

	@Test
	void customerCanBeCreatedWithoutOptionalNestedObjects() throws Exception {
		String customer = "{\"firstName\":\"NoDetails\",\"customerNumber\":2001}";

		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON).content(customer))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/customers/2001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contactDetails").value(nullValue()))
				.andExpect(jsonPath("$.customerAddress").value(nullValue()));
	}

	@Test
	void missingCustomerRequestsReturnNotFound() throws Exception {
		mockMvc.perform(get("/customers/9999"))
				.andExpect(status().isNotFound())
				.andExpect(content().string("Customer Number 9999 not found."));

		mockMvc.perform(post("/accounts/add/9999").contentType(MediaType.APPLICATION_JSON)
				.content(accountJson(5999, 100)))
				.andExpect(status().isNotFound())
				.andExpect(content().string("Customer Number 9999 not found."));

		mockMvc.perform(put("/accounts/transfer/9999").contentType(MediaType.APPLICATION_JSON)
				.content(transferJson(5999, 6000, "10")))
				.andExpect(status().isNotFound())
				.andExpect(content().string("Customer Number 9999 not found."));
	}

	@Test
	void invalidTransfersAreRejectedWithoutChangingBalances() throws Exception {
		addCustomer(1001);
		addAccount(5101, 1000);
		addAccount(5102, 50);

		mockMvc.perform(put("/accounts/transfer/1001").contentType(MediaType.APPLICATION_JSON)
				.content(transferJson(5101, 5102, "-500")))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Transfer amount must be greater than zero."));
		mockMvc.perform(put("/accounts/transfer/1001").contentType(MediaType.APPLICATION_JSON)
				.content(transferJson(5101, 5102, "0")))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Transfer amount must be greater than zero."));
		mockMvc.perform(put("/accounts/transfer/1001").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\":5101,\"toAccountNumber\":5102}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Transfer amount must be greater than zero."));
		mockMvc.perform(put("/accounts/transfer/1001").contentType(MediaType.APPLICATION_JSON)
				.content(transferJson(5101, 5101, "20")))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("From and To accounts must be different."));
		mockMvc.perform(put("/accounts/transfer/1001").contentType(MediaType.APPLICATION_JSON)
				.content(transferJson(5101, 5102, "1001")))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Insufficient Funds."));

		mockMvc.perform(get("/accounts/5101"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountBalance").value(1000.0));
		mockMvc.perform(get("/accounts/5102"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountBalance").value(50.0));
	}

	private void addCustomer(long customerNumber) throws Exception {
		String customer = "{\"firstName\":\"Test\",\"lastName\":\"Customer\",\"customerNumber\":"
				+ customerNumber + ",\"status\":\"ACTIVE\"}";
		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON).content(customer))
				.andExpect(status().isCreated());
	}

	private void addAccount(long accountNumber, double balance) throws Exception {
		mockMvc.perform(post("/accounts/add/1001").contentType(MediaType.APPLICATION_JSON)
				.content(accountJson(accountNumber, balance)))
				.andExpect(status().isCreated());
	}

	private String accountJson(long accountNumber, double balance) {
		return "{\"accountNumber\":" + accountNumber + ",\"accountType\":\"SAVINGS\","
				+ "\"accountBalance\":" + balance + ",\"accountStatus\":\"ACTIVE\","
				+ "\"bankInformation\":{\"branchCode\":1,\"branchName\":\"Centro\",\"routingNumber\":123,"
				+ "\"branchAddress\":{\"address1\":\"2 Bank\",\"city\":\"Madrid\",\"state\":\"MD\","
				+ "\"zip\":\"28001\",\"country\":\"ES\"}}}";
	}

	private String transferJson(long fromAccountNumber, long toAccountNumber, String amount) {
		return "{\"fromAccountNumber\":" + fromAccountNumber + ",\"toAccountNumber\":" + toAccountNumber
				+ ",\"transferAmount\":" + amount + "}";
	}
}

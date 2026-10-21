package com.coding.exercise.bankapp.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "bankapp.bulk.max-batch-size=5")
@AutoConfigureMockMvc
class BulkAccountControllerTests {

	private static final AtomicLong NEXT_NUMBER = new AtomicLong(700_000);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void addAccountsReportsPerItemResults() throws Exception {
		long customer = createCustomer();
		long a1 = nextNumber();
		long a2 = nextNumber();
		long existing = nextNumber();
		bulkAdd(customer, account(existing, 0)).andExpect(status().isOk());

		Map<String, Object> missingBankInfo = new HashMap<>(account(nextNumber(), 10));
		missingBankInfo.remove("bankInformation");

		bulkAdd(customer, account(a1, 100), account(a2, 0), account(a1, 5), account(existing, 1), missingBankInfo)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.operation").value("ADD"))
				.andExpect(jsonPath("$.requested").value(5))
				.andExpect(jsonPath("$.succeeded").value(2))
				.andExpect(jsonPath("$.failed").value(3))
				.andExpect(jsonPath("$.results[0].status").value("CREATED"))
				.andExpect(jsonPath("$.results[0].account.accountBalance").value(100.0))
				.andExpect(jsonPath("$.results[1].status").value("CREATED"))
				.andExpect(jsonPath("$.results[2].status").value("DUPLICATE"))
				.andExpect(jsonPath("$.results[2].message").value("Account number repeated in request."))
				.andExpect(jsonPath("$.results[3].status").value("DUPLICATE"))
				.andExpect(jsonPath("$.results[3].message").value("Account number already exists."))
				.andExpect(jsonPath("$.results[4].status").value("INVALID"));

		mockMvc.perform(get("/accounts/" + a1)).andExpect(status().isFound())
				.andExpect(jsonPath("$.accountNumber").value(a1));
	}

	@Test
	void addAccountsForUnknownCustomerIsNotFound() throws Exception {
		bulkAdd(nextNumber(), account(nextNumber(), 0))
				.andExpect(status().isNotFound())
				.andExpect(content().string(containsString("not found")));
	}

	@Test
	void emptyOrOversizedBatchIsBadRequest() throws Exception {
		long customer = createCustomer();
		perform(post("/accounts/bulk/add/" + customer), Map.of("accounts", List.of()))
				.andExpect(status().isBadRequest());
		perform(post("/accounts/bulk/lookup"), Map.of("accountNumbers", Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L)))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("maximum batch size of 5")));
	}

	@Test
	void lookupReturnsFoundAndNotFound() throws Exception {
		long customer = createCustomer();
		long a1 = nextNumber();
		long missing = nextNumber();
		bulkAdd(customer, account(a1, 42)).andExpect(status().isOk());

		perform(post("/accounts/bulk/lookup"), Map.of("accountNumbers", Arrays.asList(a1, missing, null, a1)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.succeeded").value(1))
				.andExpect(jsonPath("$.results", hasSize(4)))
				.andExpect(jsonPath("$.results[0].status").value("FOUND"))
				.andExpect(jsonPath("$.results[0].account.accountBalance").value(42.0))
				.andExpect(jsonPath("$.results[1].status").value("NOT_FOUND"))
				.andExpect(jsonPath("$.results[2].status").value("INVALID"))
				.andExpect(jsonPath("$.results[3].status").value("DUPLICATE"));
	}

	@Test
	void updateStatusOnlyTouchesCustomersOwnAccounts() throws Exception {
		long owner = createCustomer();
		long other = createCustomer();
		long mine = nextNumber();
		long theirs = nextNumber();
		bulkAdd(owner, account(mine, 10)).andExpect(status().isOk());
		bulkAdd(other, account(theirs, 10)).andExpect(status().isOk());

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("accountNumbers", List.of(mine, theirs, nextNumber()));
		body.put("accountStatus", "Frozen");
		perform(put("/accounts/bulk/status/" + owner), body)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.results[0].status").value("UPDATED"))
				.andExpect(jsonPath("$.results[0].account.accountStatus").value("Frozen"))
				.andExpect(jsonPath("$.results[1].status").value("NOT_OWNED"))
				.andExpect(jsonPath("$.results[2].status").value("NOT_FOUND"));

		perform(post("/accounts/bulk/lookup"), Map.of("accountNumbers", List.of(mine, theirs)))
				.andExpect(jsonPath("$.results[0].account.accountStatus").value("Frozen"))
				.andExpect(jsonPath("$.results[1].account.accountStatus").value("Active"));

		perform(put("/accounts/bulk/status/" + owner), Map.of("accountNumbers", List.of(mine)))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("accountStatus is required."));
	}

	@Test
	void deleteRemovesOnlyOwnedZeroBalanceAccounts() throws Exception {
		long owner = createCustomer();
		long other = createCustomer();
		long empty = nextNumber();
		long funded = nextNumber();
		long theirs = nextNumber();
		bulkAdd(owner, account(empty, 0), account(funded, 50)).andExpect(status().isOk());
		bulkAdd(other, account(theirs, 0)).andExpect(status().isOk());

		perform(post("/accounts/bulk/delete/" + owner), Map.of("accountNumbers", List.of(empty, funded, theirs)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.succeeded").value(1))
				.andExpect(jsonPath("$.results[0].status").value("DELETED"))
				.andExpect(jsonPath("$.results[1].status").value("REJECTED"))
				.andExpect(jsonPath("$.results[2].status").value("NOT_OWNED"));

		perform(post("/accounts/bulk/lookup"), Map.of("accountNumbers", List.of(empty, funded, theirs)))
				.andExpect(jsonPath("$.results[0].status").value("NOT_FOUND"))
				.andExpect(jsonPath("$.results[1].status").value("FOUND"))
				.andExpect(jsonPath("$.results[2].status").value("FOUND"));

		bulkAdd(owner, account(empty, 0))
				.andExpect(jsonPath("$.results[0].status").value("CREATED"));
	}

	@Test
	void bulkCreatedAccountsSupportTransfers() throws Exception {
		long customer = createCustomer();
		long from = nextNumber();
		long to = nextNumber();
		bulkAdd(customer, account(from, 500), account(to, 0)).andExpect(status().isOk());

		Map<String, Object> transfer = new LinkedHashMap<>();
		transfer.put("fromAccountNumber", from);
		transfer.put("toAccountNumber", to);
		transfer.put("transferAmount", 125);
		perform(put("/accounts/transfer/" + customer), transfer).andExpect(status().isOk());

		perform(post("/accounts/bulk/lookup"), Map.of("accountNumbers", List.of(from, to)))
				.andExpect(jsonPath("$.results[0].account.accountBalance").value(375.0))
				.andExpect(jsonPath("$.results[1].account.accountBalance").value(125.0));
		mockMvc.perform(get("/accounts/transactions/" + from))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].txType").value("DEBIT"));
	}

	private long createCustomer() throws Exception {
		long customerNumber = nextNumber();
		Map<String, Object> customer = new LinkedHashMap<>();
		customer.put("customerNumber", customerNumber);
		customer.put("firstName", "Test");
		customer.put("lastName", "Customer" + customerNumber);
		customer.put("status", "Active");
		customer.put("contactDetails", Map.of("emailId", "c" + customerNumber + "@test.com", "homePhone", "6150000000",
				"workPhone", "6151112222"));
		customer.put("customerAddress", address());
		perform(post("/customers/add"), customer).andExpect(status().isCreated());
		return customerNumber;
	}

	@SafeVarargs
	private ResultActions bulkAdd(long customerNumber, Map<String, Object>... accounts) throws Exception {
		return perform(post("/accounts/bulk/add/" + customerNumber), Map.of("accounts", Arrays.asList(accounts)));
	}

	private ResultActions perform(MockHttpServletRequestBuilder request, Object body) throws Exception {
		return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)));
	}

	private static Map<String, Object> account(long accountNumber, double balance) {
		Map<String, Object> bankInformation = new LinkedHashMap<>();
		bankInformation.put("branchName", "Nashville Shores");
		bankInformation.put("branchCode", 65564);
		bankInformation.put("routingNumber", 234789876);
		bankInformation.put("branchAddress", address());

		Map<String, Object> account = new LinkedHashMap<>();
		account.put("accountNumber", accountNumber);
		account.put("accountType", "Checking");
		account.put("accountStatus", "Active");
		account.put("accountBalance", balance);
		account.put("bankInformation", bankInformation);
		return account;
	}

	private static Map<String, Object> address() {
		return Map.of("address1", "123 Domain St", "city", "Hermitage", "state", "TN", "zip", "37076", "country",
				"USA");
	}

	private static long nextNumber() {
		return NEXT_NUMBER.incrementAndGet();
	}
}

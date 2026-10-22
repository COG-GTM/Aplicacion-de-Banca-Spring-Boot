package com.coding.exercise.bankapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.security.test.context.support.WithMockUser;

import com.coding.exercise.bankapp.domain.AddressDetails;
import com.coding.exercise.bankapp.domain.BulkCustomerUpdateRequest;
import com.coding.exercise.bankapp.domain.ContactDetails;
import com.coding.exercise.bankapp.domain.CustomerDetails;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser
class BulkCustomerUpdateTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void seedCustomers() throws Exception {
		mockMvc.perform(delete("/customers/1000"));
		mockMvc.perform(delete("/customers/1001"));
		addCustomer("AddCustomer_1000.json");
		addCustomer("AddCustomer_1001.json");
	}

	@Test
	void mixedBatchUpdatesOnlyValidUniqueCustomerAndPreservesUnspecifiedFields() throws Exception {
		BulkCustomerUpdateRequest request = BulkCustomerUpdateRequest.builder()
				.customers(Arrays.asList(
						CustomerDetails.builder()
								.customerNumber(1000L)
								.lastName("Updated")
								.contactDetails(ContactDetails.builder().emailId("updated@test.com").build())
								.build(),
						CustomerDetails.builder().customerNumber(9999L).lastName("Missing").build(),
						CustomerDetails.builder().lastName("Missing number").build(),
						CustomerDetails.builder().customerNumber(1000L).lastName("Duplicate").build(),
						CustomerDetails.builder().customerNumber(1001L).build()))
				.build();

		mockMvc.perform(put("/customers/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requested").value(5))
				.andExpect(jsonPath("$.updated").value(1))
				.andExpect(jsonPath("$.failed").value(4))
				.andExpect(jsonPath("$.results[0].status").value("UPDATED"))
				.andExpect(jsonPath("$.results[1].status").value("NOT_FOUND"))
				.andExpect(jsonPath("$.results[2].status").value("INVALID"))
				.andExpect(jsonPath("$.results[3].status").value("DUPLICATE"))
				.andExpect(jsonPath("$.results[4].status").value("INVALID"));

		mockMvc.perform(get("/customers/1000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lastName").value("Updated"))
				.andExpect(jsonPath("$.contactDetails.emailId").value("updated@test.com"))
				.andExpect(jsonPath("$.firstName").value("John"))
				.andExpect(jsonPath("$.contactDetails.homePhone").value("6150000000"));
	}

	@Test
	void updatesMultipleValidCustomersAndPersistsChanges() throws Exception {
		BulkCustomerUpdateRequest request = BulkCustomerUpdateRequest.builder()
				.customers(Arrays.asList(
						CustomerDetails.builder().customerNumber(1000L).status("Inactive").build(),
						CustomerDetails.builder().customerNumber(1001L).status("Inactive").build()))
				.build();

		mockMvc.perform(put("/customers/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requested").value(2))
				.andExpect(jsonPath("$.updated").value(2))
				.andExpect(jsonPath("$.failed").value(0))
				.andExpect(jsonPath("$.results[0].status").value("UPDATED"))
				.andExpect(jsonPath("$.results[1].status").value("UPDATED"));

		mockMvc.perform(get("/customers/1000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("Inactive"));
		mockMvc.perform(get("/customers/1001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("Inactive"));
	}

	@Test
	void treatsEmptyNestedObjectsAsNoFieldsToUpdate() throws Exception {
		BulkCustomerUpdateRequest request = BulkCustomerUpdateRequest.builder()
				.customers(Arrays.asList(
						CustomerDetails.builder()
								.customerNumber(1000L)
								.contactDetails(ContactDetails.builder().build())
								.build(),
						CustomerDetails.builder()
								.customerNumber(1001L)
								.customerAddress(AddressDetails.builder().build())
								.build()))
				.build();

		mockMvc.perform(put("/customers/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requested").value(2))
				.andExpect(jsonPath("$.updated").value(0))
				.andExpect(jsonPath("$.failed").value(2))
				.andExpect(jsonPath("$.results[0].status").value("INVALID"))
				.andExpect(jsonPath("$.results[0].message").value("No fields to update."))
				.andExpect(jsonPath("$.results[1].status").value("INVALID"))
				.andExpect(jsonPath("$.results[1].message").value("No fields to update."));
	}

	@Test
	void rejectsOverlengthFieldWithoutAffectingOtherUpdates() throws Exception {
		BulkCustomerUpdateRequest request = BulkCustomerUpdateRequest.builder()
				.customers(Arrays.asList(
						CustomerDetails.builder().customerNumber(1000L).lastName("Smith").build(),
						CustomerDetails.builder().customerNumber(1001L).lastName("x".repeat(256)).build()))
				.build();

		mockMvc.perform(put("/customers/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requested").value(2))
				.andExpect(jsonPath("$.updated").value(1))
				.andExpect(jsonPath("$.failed").value(1))
				.andExpect(jsonPath("$.results[0].status").value("UPDATED"))
				.andExpect(jsonPath("$.results[1].status").value("INVALID"))
				.andExpect(jsonPath("$.results[1].message").value("lastName exceeds 255 characters."));

		mockMvc.perform(get("/customers/1000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lastName").value("Smith"));
		mockMvc.perform(get("/customers/1001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lastName").value("Federer"));
	}

	@Test
	void rejectsEmptyBatch() throws Exception {
		BulkCustomerUpdateRequest request = BulkCustomerUpdateRequest.builder()
				.customers(Collections.emptyList())
				.build();

		mockMvc.perform(put("/customers/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content()
						.string("Bulk update request must contain at least one customer."));
	}

	@Test
	void rejectsMoreThanOneHundredItems() throws Exception {
		List<CustomerDetails> customers = new ArrayList<>();
		for (long customerNumber = 1; customerNumber <= 101; customerNumber++) {
			customers.add(CustomerDetails.builder().customerNumber(customerNumber).status("Active").build());
		}
		BulkCustomerUpdateRequest request = BulkCustomerUpdateRequest.builder().customers(customers).build();

		mockMvc.perform(put("/customers/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content()
						.string("Bulk update request cannot exceed 100 customers."));
	}

	@Test
	void rejectsRequestWithoutCustomers() throws Exception {
		mockMvc.perform(put("/customers/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content()
						.string("Bulk update request must contain at least one customer."));
	}

	private void addCustomer(String fixtureName) throws Exception {
		mockMvc.perform(post("/customers/add")
				.contentType(MediaType.APPLICATION_JSON)
				.content(fixture(fixtureName)))
				.andExpect(status().isCreated());
	}

	private String fixture(String fixtureName) throws IOException {
		try (java.io.InputStream input = new ClassPathResource(fixtureName).getInputStream()) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}

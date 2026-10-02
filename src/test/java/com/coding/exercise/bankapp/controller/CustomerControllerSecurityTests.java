package com.coding.exercise.bankapp.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerControllerSecurityTests {

	private static final String CUSTOMER_JSON = "{\"firstName\":\"Jane\","
			+ "\"contactDetails\":{\"emailId\":\"jane@test.com\"},"
			+ "\"customerAddress\":{\"city\":\"Hermitage\"}}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void anonymousCallerIsRejected() throws Exception {
		mockMvc.perform(get("/customers/all")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/customers/1000")).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/customers/1000").contentType(MediaType.APPLICATION_JSON).content(CUSTOMER_JSON))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/customers/1000")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "1000", roles = "USER")
	public void customerCannotListAllCustomers() throws Exception {
		mockMvc.perform(get("/customers/all")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "1000", roles = "USER")
	public void customerCannotAccessAnotherCustomer() throws Exception {
		mockMvc.perform(get("/customers/1001")).andExpect(status().isForbidden());
		mockMvc.perform(put("/customers/1001").contentType(MediaType.APPLICATION_JSON).content(CUSTOMER_JSON))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/customers/1001")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "1000", roles = "USER")
	public void customerCanAccessOwnRecord() throws Exception {
		mockMvc.perform(get("/customers/1000")).andExpect(status().isOk());
		mockMvc.perform(put("/customers/1000").contentType(MediaType.APPLICATION_JSON).content(CUSTOMER_JSON))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/customers/1000")).andExpect(status().isBadRequest());
	}

	@Test
	public void adminCanAccessAllCustomers() throws Exception {
		mockMvc.perform(get("/customers/all").with(httpBasic("bankapp", "changeit"))).andExpect(status().isOk());
		mockMvc.perform(get("/customers/1001").with(httpBasic("bankapp", "changeit"))).andExpect(status().isOk());
	}

	@Test
	public void invalidCredentialsAreRejected() throws Exception {
		mockMvc.perform(get("/customers/all").with(httpBasic("bankapp", "wrong"))).andExpect(status().isUnauthorized());
	}
}

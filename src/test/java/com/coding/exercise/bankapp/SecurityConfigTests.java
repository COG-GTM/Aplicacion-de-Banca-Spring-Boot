package com.coding.exercise.bankapp;

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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void unauthenticatedRequestToCustomersIsRejected() throws Exception {
		mockMvc.perform(get("/customers/all")).andExpect(status().isUnauthorized());
	}

	@Test
	public void unauthenticatedTransferIsRejected() throws Exception {
		mockMvc.perform(put("/accounts/transfer/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void unauthenticatedDeleteIsRejected() throws Exception {
		mockMvc.perform(delete("/customers/1")).andExpect(status().isUnauthorized());
	}

	@Test
	public void unauthenticatedH2ConsoleIsRejected() throws Exception {
		mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
	}

	@Test
	public void wrongCredentialsAreRejected() throws Exception {
		mockMvc.perform(get("/customers/all").with(httpBasic("test-user", "wrong")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void authenticatedRequestToCustomersSucceeds() throws Exception {
		mockMvc.perform(get("/customers/all").with(httpBasic("test-user", "test-password")))
				.andExpect(status().isOk());
	}

	@Test
	public void healthCheckIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}

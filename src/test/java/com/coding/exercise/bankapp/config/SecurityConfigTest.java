package com.coding.exercise.bankapp.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = { "spring.security.user.name=test-user",
		"spring.security.user.password=test-only-secret" })
@AutoConfigureMockMvc
public class SecurityConfigTest {

	private static final String USER = "test-user";
	private static final String PASSWORD = "test-only-secret";

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void unauthenticatedCustomerListIsRejected() throws Exception {
		mockMvc.perform(get("/customers/all")).andExpect(status().isUnauthorized())
				.andExpect(header().exists("WWW-Authenticate"));
	}

	@Test
	public void wrongPasswordIsRejected() throws Exception {
		mockMvc.perform(get("/customers/all").with(httpBasic(USER, "wrong"))).andExpect(status().isUnauthorized());
	}

	@Test
	public void authenticatedCustomerListIsAllowed() throws Exception {
		mockMvc.perform(get("/customers/all").with(httpBasic(USER, PASSWORD))).andExpect(status().isOk());
	}

	@Test
	public void unauthenticatedMutationsAreRejected() throws Exception {
		mockMvc.perform(put("/accounts/transfer/1").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\":1,\"toAccountNumber\":2,\"transferAmount\":10}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/accounts/add/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/customers/1")).andExpect(status().isUnauthorized());
	}

	@Test
	public void unauthenticatedAccountReadsAreRejected() throws Exception {
		mockMvc.perform(get("/accounts/1")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/accounts/transactions/1")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/customers/1")).andExpect(status().isUnauthorized());
	}

	@Test
	public void h2ConsoleRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
	}

	@Test
	public void healthProbeStaysPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}

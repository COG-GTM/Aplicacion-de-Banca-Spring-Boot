package com.coding.exercise.bankapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

	private static final String CUSTOMER_JSON = "{"
			+ "\"firstName\":\"Grace\",\"lastName\":\"Hopper\",\"middleName\":\"B\","
			+ "\"customerNumber\":2001,\"status\":\"Active\","
			+ "\"customerAddress\":{\"address1\":\"3 Navy Rd\",\"address2\":\"\",\"city\":\"Arlington\","
			+ "\"state\":\"VA\",\"zip\":\"22201\",\"country\":\"US\"},"
			+ "\"contactDetails\":{\"emailId\":\"grace@example.com\",\"homePhone\":\"333\",\"workPhone\":\"444\"}"
			+ "}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rootIsNotUnauthorizedOrForbidden() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
	}

	@Test
	void h2ConsoleIsReachableWithoutCredentials() throws Exception {
		mockMvc.perform(get("/h2-console/"))
				.andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));

		mockMvc.perform(get("/h2-console/login.jsp"))
				.andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
	}

	@Test
	void postWithoutCsrfTokenIsNotRejected() throws Exception {
		mockMvc.perform(post("/customers/add")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CUSTOMER_JSON))
				.andExpect(status().isCreated());
	}

	@Test
	void framesNotDenied() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(result -> assertThat(result.getResponse().getHeader("X-Frame-Options")).isNull());
	}
}

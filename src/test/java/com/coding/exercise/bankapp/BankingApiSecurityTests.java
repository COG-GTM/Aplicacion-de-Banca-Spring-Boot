package com.coding.exercise.bankapp;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
// csrf() swaps the filter's token repository for a test one, so each test needs a fresh context
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class BankingApiSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	private static String json(String resource) throws Exception {
		return new String(new ClassPathResource(resource).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	}

	@Test
	public void stateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
		mockMvc.perform(post("/customers/add").contentType(MediaType.APPLICATION_JSON)
				.content(json("AddCustomer_1002.json")))
				.andExpect(status().isForbidden());
	}

	@Test
	public void customerAndAccountFlowWorksWithCsrfToken() throws Exception {
		mockMvc.perform(post("/customers/add").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json("AddCustomer_1000.json")))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/customers/1000"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("John")));

		mockMvc.perform(post("/accounts/add/1000").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json("addAccountInformation5000_Customer1000.json")))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/accounts/5000"))
				.andExpect(status().isFound())
				.andExpect(content().string(containsString("5000")));
	}

	@Test
	public void readRequestIssuesCsrfCookie() throws Exception {
		mockMvc.perform(get("/customers/all"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("XSRF-TOKEN=")));
	}

	@Test
	public void csrfCookieIssuedOnReadIsAcceptedOnWrite() throws Exception {
		Cookie xsrfCookie = mockMvc.perform(get("/customers/all")).andReturn().getResponse().getCookie("XSRF-TOKEN");

		mockMvc.perform(post("/customers/add").cookie(xsrfCookie).header("X-XSRF-TOKEN", xsrfCookie.getValue())
				.contentType(MediaType.APPLICATION_JSON).content(json("AddCustomer_1001.json")))
				.andExpect(status().isCreated());
	}

	@Test
	public void trailingSlashUrlStillMatches() throws Exception {
		mockMvc.perform(get("/customers/all/")).andExpect(status().isOk());
	}

	@Test
	public void openApiDocsAreServed() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("BANKING APPLICATION REST API")))
				.andExpect(jsonPath("$.components.securitySchemes.csrf.name").value("X-XSRF-TOKEN"))
				.andExpect(jsonPath("$.paths['/customers/add'].post.security[0].csrf").exists())
				.andExpect(jsonPath("$.paths['/customers/all'].get.security").doesNotExist());
	}
}

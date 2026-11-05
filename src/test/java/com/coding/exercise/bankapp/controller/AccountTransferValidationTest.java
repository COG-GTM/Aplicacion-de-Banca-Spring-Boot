package com.coding.exercise.bankapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.coding.exercise.bankapp.domain.TransferDetails;
import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.model.Customer;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;
import com.coding.exercise.bankapp.repository.TransactionRepository;
import com.coding.exercise.bankapp.service.BankingServiceImpl;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AccountTransferValidationTest {

	private static final long CUSTOMER_NUMBER = 7001L;
	private static final long FROM_ACCOUNT = 9000L;
	private static final long TO_ACCOUNT = 9001L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private BankingServiceImpl bankingService;

	@BeforeEach
	void seed() {
		customerRepository.save(Customer.builder().customerNumber(CUSTOMER_NUMBER).firstName("Mallory")
				.status("ACTIVE").createDateTime(new Date()).build());
		accountRepository.save(account(FROM_ACCOUNT, 0.0));
		accountRepository.save(account(TO_ACCOUNT, 500.0));
	}

	@ParameterizedTest
	@ValueSource(strings = { "-1000000", "-100", "0", "0.00", "\"NaN\"", "\"Infinity\"", "\"-Infinity\"", "null",
			"1.005", "1.000", "1e400", "\"abc\"" })
	void rejectsInvalidAmountsWithoutChangingBalances(String amountJson) throws Exception {
		mockMvc.perform(transfer(FROM_ACCOUNT, TO_ACCOUNT, amountJson)).andExpect(status().isBadRequest());

		assertBalancesUnchanged();
	}

	@Test
	void rejectsMissingAccountNumbers() throws Exception {
		mockMvc.perform(put("/accounts/transfer/" + CUSTOMER_NUMBER).contentType(MediaType.APPLICATION_JSON)
				.content("{\"toAccountNumber\": " + TO_ACCOUNT + ", \"transferAmount\": 10}"))
				.andExpect(status().isBadRequest());

		assertBalancesUnchanged();
	}

	@Test
	void rejectsSameAccountTransfer() throws Exception {
		mockMvc.perform(transfer(TO_ACCOUNT, TO_ACCOUNT, "100")).andExpect(status().isBadRequest());

		assertBalancesUnchanged();
	}

	@Test
	void rejectsInsufficientFunds() throws Exception {
		mockMvc.perform(transfer(FROM_ACCOUNT, TO_ACCOUNT, "0.01")).andExpect(status().isBadRequest());

		assertBalancesUnchanged();
	}

	@Test
	void serviceRejectsInvalidAmountsWhenCalledDirectly() {
		for (String amount : new String[] { "-1000000", "0", "1.005", "1.000", "1e20" }) {
			ResponseEntity<Object> response = bankingService.transferDetails(
					new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, new BigDecimal(amount)), CUSTOMER_NUMBER);
			assertThat(response.getStatusCode()).as(amount).isEqualTo(HttpStatus.BAD_REQUEST);
		}
		assertThat(bankingService.transferDetails(new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, null), CUSTOMER_NUMBER)
				.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(bankingService.transferDetails(
				new TransferDetails(TO_ACCOUNT, TO_ACCOUNT, BigDecimal.TEN), CUSTOMER_NUMBER).getStatusCode())
				.isEqualTo(HttpStatus.BAD_REQUEST);

		assertBalancesUnchanged();
	}

	private void assertBalancesUnchanged() {
		assertThat(balance(FROM_ACCOUNT)).isEqualTo(0.0);
		assertThat(balance(TO_ACCOUNT)).isEqualTo(500.0);
		assertThat(transactionRepository.findByAccountNumber(FROM_ACCOUNT).get()).isEmpty();
		assertThat(transactionRepository.findByAccountNumber(TO_ACCOUNT).get()).isEmpty();
	}

	private double balance(long accountNumber) {
		return accountRepository.findByAccountNumber(accountNumber).get().getAccountBalance();
	}

	private static Account account(long accountNumber, double balance) {
		return Account.builder().accountNumber(accountNumber).accountStatus("ACTIVE").accountType("SAVINGS")
				.accountBalance(balance).createDateTime(new Date()).build();
	}

	private static org.springframework.test.web.servlet.RequestBuilder transfer(long from, long to,
			String amountJson) {
		return put("/accounts/transfer/" + CUSTOMER_NUMBER).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\": " + from + ", \"toAccountNumber\": " + to
						+ ", \"transferAmount\": " + amountJson + "}");
	}
}

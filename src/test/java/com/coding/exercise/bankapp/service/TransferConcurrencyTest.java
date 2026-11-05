package com.coding.exercise.bankapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.coding.exercise.bankapp.domain.TransferDetails;
import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.model.Customer;
import com.coding.exercise.bankapp.model.CustomerAccountXRef;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerAccountXRefRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:transfer-concurrency;LOCK_TIMEOUT=30000")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TransferConcurrencyTest {

	private static final int PARALLEL_TRANSFERS = 20;
	private static final long CUSTOMER_NUMBER = 4242L;
	private static final long SOURCE_ACCOUNT = 9000L;
	private static final long FIRST_DESTINATION = 9100L;
	private static final double BALANCE = 100.0;
	private static final long OTHER_CUSTOMER_NUMBER = 5353L;
	private static final long OTHER_ACCOUNT = 9500L;

	@Autowired
	private BankingService bankingService;
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	private CustomerRepository customerRepository;
	@Autowired
	private CustomerAccountXRefRepository custAccXRefRepository;

	@BeforeEach
	public void setUp() {
		customerRepository.save(Customer.builder().customerNumber(CUSTOMER_NUMBER).firstName("Test").lastName("User")
				.status("ACTIVE").createDateTime(new Date()).build());
		accountRepository.save(account(SOURCE_ACCOUNT, BALANCE));
		custAccXRefRepository.save(CustomerAccountXRef.builder().customerNumber(CUSTOMER_NUMBER)
				.accountNumber(SOURCE_ACCOUNT).build());
		customerRepository.save(Customer.builder().customerNumber(OTHER_CUSTOMER_NUMBER).firstName("Other")
				.lastName("User").status("ACTIVE").createDateTime(new Date()).build());
		accountRepository.save(account(OTHER_ACCOUNT, BALANCE));
		custAccXRefRepository.save(CustomerAccountXRef.builder().customerNumber(OTHER_CUSTOMER_NUMBER)
				.accountNumber(OTHER_ACCOUNT).build());
		for (int i = 0; i < PARALLEL_TRANSFERS; i++) {
			accountRepository.save(account(FIRST_DESTINATION + i, 0.0));
		}
	}

	@Test
	public void parallelFullBalanceTransfersSucceedExactlyOnce() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_TRANSFERS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<ResponseEntity<Object>>> results = new ArrayList<>();
		try {
			for (int i = 0; i < PARALLEL_TRANSFERS; i++) {
				long destination = FIRST_DESTINATION + i;
				Callable<ResponseEntity<Object>> transfer = () -> {
					start.await();
					return bankingService.transferDetails(new TransferDetails(SOURCE_ACCOUNT, destination, BALANCE),
							CUSTOMER_NUMBER);
				};
				results.add(pool.submit(transfer));
			}
			start.countDown();

			int succeeded = 0;
			int insufficientFunds = 0;
			for (Future<ResponseEntity<Object>> result : results) {
				ResponseEntity<Object> response = result.get(60, TimeUnit.SECONDS);
				if (response.getStatusCode() == HttpStatus.OK) {
					succeeded++;
				} else if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
					insufficientFunds++;
				}
			}

			double sourceBalance = accountRepository.findByAccountNumber(SOURCE_ACCOUNT).get().getAccountBalance();
			double credited = 0;
			for (int i = 0; i < PARALLEL_TRANSFERS; i++) {
				credited += accountRepository.findByAccountNumber(FIRST_DESTINATION + i).get().getAccountBalance();
			}

			assertEquals(1, succeeded, "exactly one full-balance transfer must succeed");
			assertEquals(PARALLEL_TRANSFERS - 1, insufficientFunds);
			assertTrue(sourceBalance >= 0, "source balance must never go negative");
			assertEquals(0.0, sourceBalance);
			assertEquals(BALANCE, credited, "total credited must equal the original balance");
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	public void nonPositiveTransferAmountIsRejected() {
		for (double amount : new double[] { -50.0, 0.0, Double.NaN }) {
			ResponseEntity<Object> response = bankingService
					.transferDetails(new TransferDetails(SOURCE_ACCOUNT, FIRST_DESTINATION, amount), CUSTOMER_NUMBER);
			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		}
		assertEquals(BALANCE, accountRepository.findByAccountNumber(SOURCE_ACCOUNT).get().getAccountBalance());
		assertEquals(0.0, accountRepository.findByAccountNumber(FIRST_DESTINATION).get().getAccountBalance());
	}

	@Test
	public void cannotDebitAnotherCustomersAccount() {
		ResponseEntity<Object> response = bankingService
				.transferDetails(new TransferDetails(OTHER_ACCOUNT, FIRST_DESTINATION, BALANCE), CUSTOMER_NUMBER);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals(BALANCE, accountRepository.findByAccountNumber(OTHER_ACCOUNT).get().getAccountBalance());
		assertEquals(0.0, accountRepository.findByAccountNumber(FIRST_DESTINATION).get().getAccountBalance());
	}

	private static Account account(long accountNumber, double balance) {
		return Account.builder().accountNumber(accountNumber).accountBalance(balance).accountStatus("ACTIVE")
				.accountType("SAVINGS").createDateTime(new Date()).build();
	}
}

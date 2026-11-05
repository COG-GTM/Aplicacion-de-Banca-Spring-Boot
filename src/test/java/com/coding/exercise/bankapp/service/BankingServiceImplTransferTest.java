package com.coding.exercise.bankapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.coding.exercise.bankapp.domain.TransferDetails;
import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.model.Customer;
import com.coding.exercise.bankapp.model.Transaction;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;
import com.coding.exercise.bankapp.repository.TransactionRepository;
import com.coding.exercise.bankapp.service.helper.BankingServiceHelper;

class BankingServiceImplTransferTest {

	private static final Long CUSTOMER_NUMBER = 1000L;
	private static final Long FROM_ACCOUNT = 9000L;
	private static final Long TO_ACCOUNT = 9001L;

	private CustomerRepository customerRepository;
	private AccountRepository accountRepository;
	private TransactionRepository transactionRepository;
	private BankingServiceImpl bankingService;
	private Account fromAccount;
	private Account toAccount;

	@BeforeEach
	void setUp() {
		customerRepository = mock(CustomerRepository.class);
		accountRepository = mock(AccountRepository.class);
		transactionRepository = mock(TransactionRepository.class);
		BankingServiceHelper helper = mock(BankingServiceHelper.class);
		when(helper.createTransaction(any(), any(), any())).thenReturn(new Transaction());

		bankingService = new BankingServiceImpl(customerRepository);
		ReflectionTestUtils.setField(bankingService, "accountRepository", accountRepository);
		ReflectionTestUtils.setField(bankingService, "transactionRepository", transactionRepository);
		ReflectionTestUtils.setField(bankingService, "bankingServiceHelper", helper);

		fromAccount = Account.builder().accountNumber(FROM_ACCOUNT).accountBalance(100.0).build();
		toAccount = Account.builder().accountNumber(TO_ACCOUNT).accountBalance(50.0).build();

		when(customerRepository.findByCustomerNumber(CUSTOMER_NUMBER)).thenReturn(Optional.of(new Customer()));
		when(accountRepository.findByAccountNumber(FROM_ACCOUNT)).thenReturn(Optional.of(fromAccount));
		when(accountRepository.findByAccountNumber(TO_ACCOUNT)).thenReturn(Optional.of(toAccount));
	}

	@Test
	void rejectsNegativeAmount() {
		assertRejected(new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, -100.0));
	}

	@Test
	void rejectsZeroAmount() {
		assertRejected(new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, 0.0));
	}

	@Test
	void rejectsNaNAmount() {
		assertRejected(new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, Double.NaN));
	}

	@Test
	void rejectsInfiniteAmount() {
		assertRejected(new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, Double.POSITIVE_INFINITY));
		assertRejected(new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, Double.NEGATIVE_INFINITY));
	}

	@Test
	void rejectsNullAmount() {
		assertRejected(new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, null));
	}

	@Test
	void rejectsSameAccountTransfer() {
		assertRejected(new TransferDetails(FROM_ACCOUNT, FROM_ACCOUNT, 10.0));
	}

	@Test
	void rejectsInsufficientFunds() {
		assertRejected(new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, 100.01));
	}

	@Test
	void transfersValidAmount() {
		ResponseEntity<Object> response = bankingService.transferDetails(
				new TransferDetails(FROM_ACCOUNT, TO_ACCOUNT, 40.0), CUSTOMER_NUMBER);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(60.0, fromAccount.getAccountBalance());
		assertEquals(90.0, toAccount.getAccountBalance());
	}

	private void assertRejected(TransferDetails transferDetails) {
		ResponseEntity<Object> response = bankingService.transferDetails(transferDetails, CUSTOMER_NUMBER);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(100.0, fromAccount.getAccountBalance());
		assertEquals(50.0, toAccount.getAccountBalance());
		verify(accountRepository, never()).saveAll(anyIterable());
		verify(transactionRepository, never()).save(any());
	}
}

package com.coding.exercise.bankapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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

	private static final long CUSTOMER = 7001L;
	private static final long FROM = 9000L;
	private static final long TO = 9001L;

	private AccountRepository accountRepository;
	private TransactionRepository transactionRepository;
	private BankingServiceImpl service;
	private Account fromAccount;
	private Account toAccount;

	@BeforeEach
	void setUp() {
		CustomerRepository customerRepository = mock(CustomerRepository.class);
		accountRepository = mock(AccountRepository.class);
		transactionRepository = mock(TransactionRepository.class);
		service = new BankingServiceImpl(customerRepository);
		ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
		ReflectionTestUtils.setField(service, "transactionRepository", transactionRepository);
		ReflectionTestUtils.setField(service, "bankingServiceHelper", new BankingServiceHelper());

		fromAccount = Account.builder().accountNumber(FROM).accountBalance(0.0).build();
		toAccount = Account.builder().accountNumber(TO).accountBalance(500.0).build();
		when(customerRepository.findByCustomerNumber(CUSTOMER))
				.thenReturn(Optional.of(Customer.builder().customerNumber(CUSTOMER).build()));
		when(accountRepository.findByAccountNumber(FROM)).thenReturn(Optional.of(fromAccount));
		when(accountRepository.findByAccountNumber(TO)).thenReturn(Optional.of(toAccount));
	}

	@ParameterizedTest
	@ValueSource(strings = { "-1000000", "-0.01", "0", "1.005", "1.000", "1e20" })
	void rejectsInvalidAmountWithoutTouchingBalances(String amount) {
		ResponseEntity<Object> response = service.transferDetails(new TransferDetails(FROM, TO, new BigDecimal(amount)),
				CUSTOMER);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertNothingPersisted();
	}

	@Test
	void rejectsNullAmountAndSameAccount() {
		assertThat(service.transferDetails(new TransferDetails(FROM, TO, null), CUSTOMER).getStatusCode())
				.isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(service.transferDetails(new TransferDetails(TO, TO, BigDecimal.TEN), CUSTOMER).getStatusCode())
				.isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(service.transferDetails(new TransferDetails(null, TO, BigDecimal.TEN), CUSTOMER).getStatusCode())
				.isEqualTo(HttpStatus.BAD_REQUEST);
		assertNothingPersisted();
	}

	@Test
	void rejectsInsufficientFunds() {
		assertThat(service.transferDetails(new TransferDetails(FROM, TO, new BigDecimal("0.01")), CUSTOMER)
				.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertNothingPersisted();
	}

	@Test
	void movesValidAmountFromSourceToDestination() {
		ResponseEntity<Object> response = service.transferDetails(new TransferDetails(TO, FROM, new BigDecimal("100.10")),
				CUSTOMER);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(toAccount.getAccountBalance()).isEqualTo(399.90);
		assertThat(fromAccount.getAccountBalance()).isEqualTo(100.10);
		ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository, times(2)).save(tx.capture());
		assertThat(tx.getAllValues()).extracting(Transaction::getTxAmount).containsOnly(100.10);
		assertThat(tx.getAllValues()).extracting(Transaction::getTxType).containsExactly("DEBIT", "CREDIT");
	}

	private void assertNothingPersisted() {
		assertThat(fromAccount.getAccountBalance()).isEqualTo(0.0);
		assertThat(toAccount.getAccountBalance()).isEqualTo(500.0);
		verify(accountRepository, never()).saveAll(anyIterable());
		verify(transactionRepository, never()).save(any());
	}
}

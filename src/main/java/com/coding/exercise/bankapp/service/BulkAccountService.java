package com.coding.exercise.bankapp.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.coding.exercise.bankapp.domain.AccountInformation;
import com.coding.exercise.bankapp.domain.AddressDetails;
import com.coding.exercise.bankapp.domain.BankInformation;
import com.coding.exercise.bankapp.domain.bulk.BulkAccountNumbersRequest;
import com.coding.exercise.bankapp.domain.bulk.BulkAccountRequest;
import com.coding.exercise.bankapp.domain.bulk.BulkAccountStatusRequest;
import com.coding.exercise.bankapp.domain.bulk.BulkItemResult;
import com.coding.exercise.bankapp.domain.bulk.BulkItemStatus;
import com.coding.exercise.bankapp.domain.bulk.BulkOperationResponse;
import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.model.CustomerAccountXRef;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerAccountXRefRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;
import com.coding.exercise.bankapp.service.helper.BankingServiceHelper;

/**
 * Bulk management of account numbers. Every item in a batch is processed
 * independently and reported with its own {@link BulkItemStatus}; only
 * request-level problems (empty/oversized batch, unknown customer) fail the
 * whole call.
 */
@Service
@Transactional
public class BulkAccountService {

	private static final int MAX_TEXT_LENGTH = 255;

	private final CustomerRepository customerRepository;
	private final AccountRepository accountRepository;
	private final CustomerAccountXRefRepository custAccXRefRepository;
	private final BankingServiceHelper bankingServiceHelper;
	private final int maxBatchSize;

	public BulkAccountService(CustomerRepository customerRepository, AccountRepository accountRepository,
			CustomerAccountXRefRepository custAccXRefRepository, BankingServiceHelper bankingServiceHelper,
			@Value("${bankapp.bulk.max-batch-size:100}") int maxBatchSize) {
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
		this.custAccXRefRepository = custAccXRefRepository;
		this.bankingServiceHelper = bankingServiceHelper;
		this.maxBatchSize = maxBatchSize;
	}

	public BulkOperationResponse addAccounts(Long customerNumber, BulkAccountRequest request) {
		List<AccountInformation> accounts = request == null ? null : request.getAccounts();
		validateBatch(accounts, "accounts");
		requireCustomer(customerNumber);

		Set<Long> existing = findAccounts(accounts.stream()
				.filter(Objects::nonNull)
				.map(AccountInformation::getAccountNumber)
				.collect(Collectors.toList()), false).keySet();

		Set<Long> seen = new HashSet<>();
		List<BulkItemResult> results = new ArrayList<>();
		for (AccountInformation accountInformation : accounts) {
			Long accountNumber = accountInformation == null ? null : accountInformation.getAccountNumber();
			String invalidReason = validateNewAccount(accountInformation);
			if (invalidReason != null) {
				results.add(result(accountNumber, BulkItemStatus.INVALID, invalidReason));
			} else if (!seen.add(accountNumber)) {
				results.add(result(accountNumber, BulkItemStatus.DUPLICATE, "Account number repeated in request."));
			} else if (existing.contains(accountNumber)) {
				results.add(result(accountNumber, BulkItemStatus.DUPLICATE, "Account number already exists."));
			} else {
				Account account = bankingServiceHelper.convertToAccountEntity(accountInformation);
				if (account.getAccountBalance() == null) {
					account.setAccountBalance(0.0);
				}
				account.setCreateDateTime(new Date());
				accountRepository.save(account);
				custAccXRefRepository.save(CustomerAccountXRef.builder()
						.accountNumber(accountNumber)
						.customerNumber(customerNumber)
						.build());
				results.add(BulkItemResult.builder()
						.accountNumber(accountNumber)
						.status(BulkItemStatus.CREATED)
						.account(bankingServiceHelper.convertToAccountDomain(account))
						.build());
			}
		}
		return BulkOperationResponse.of("ADD", results);
	}

	@Transactional(readOnly = true)
	public BulkOperationResponse lookupAccounts(BulkAccountNumbersRequest request) {
		List<Long> accountNumbers = request == null ? null : request.getAccountNumbers();
		validateBatch(accountNumbers, "accountNumbers");

		Map<Long, Account> accounts = findAccounts(accountNumbers, false);
		return BulkOperationResponse.of("LOOKUP", processNumbers(accountNumbers, accountNumber -> {
			Account account = accounts.get(accountNumber);
			if (account == null) {
				return result(accountNumber, BulkItemStatus.NOT_FOUND, "Account number not found.");
			}
			return BulkItemResult.builder()
					.accountNumber(accountNumber)
					.status(BulkItemStatus.FOUND)
					.account(bankingServiceHelper.convertToAccountDomain(account))
					.build();
		}));
	}

	public BulkOperationResponse updateAccountStatus(Long customerNumber, BulkAccountStatusRequest request) {
		List<Long> accountNumbers = request == null ? null : request.getAccountNumbers();
		validateBatch(accountNumbers, "accountNumbers");
		if (!StringUtils.hasText(request.getAccountStatus())) {
			throw new BulkRequestException(HttpStatus.BAD_REQUEST, "accountStatus is required.");
		}
		if (tooLong(request.getAccountStatus())) {
			throw new BulkRequestException(HttpStatus.BAD_REQUEST,
					"accountStatus must be at most " + MAX_TEXT_LENGTH + " characters.");
		}
		requireCustomer(customerNumber);

		Map<Long, Account> accounts = findAccounts(accountNumbers, false);
		Set<Long> owned = findOwnedAccountNumbers(customerNumber, accountNumbers);
		Date now = new Date();
		return BulkOperationResponse.of("UPDATE_STATUS", processNumbers(accountNumbers, accountNumber -> {
			BulkItemResult notManageable = checkManageable(accountNumber, accounts, owned, customerNumber);
			if (notManageable != null) {
				return notManageable;
			}
			Account account = accounts.get(accountNumber);
			account.setAccountStatus(request.getAccountStatus());
			account.setUpdateDateTime(now);
			accountRepository.save(account);
			return BulkItemResult.builder()
					.accountNumber(accountNumber)
					.status(BulkItemStatus.UPDATED)
					.account(bankingServiceHelper.convertToAccountDomain(account))
					.build();
		}));
	}

	public BulkOperationResponse deleteAccounts(Long customerNumber, BulkAccountNumbersRequest request) {
		List<Long> accountNumbers = request == null ? null : request.getAccountNumbers();
		validateBatch(accountNumbers, "accountNumbers");
		requireCustomer(customerNumber);

		Map<Long, Account> accounts = findAccounts(accountNumbers, true);
		Set<Long> owned = findOwnedAccountNumbers(customerNumber, accountNumbers);
		List<Long> deleted = new ArrayList<>();
		List<BulkItemResult> results = processNumbers(accountNumbers, accountNumber -> {
			BulkItemResult notManageable = checkManageable(accountNumber, accounts, owned, customerNumber);
			if (notManageable != null) {
				return notManageable;
			}
			Account account = accounts.get(accountNumber);
			if (account.getAccountBalance() == null || account.getAccountBalance() != 0) {
				return result(accountNumber, BulkItemStatus.REJECTED,
						"Account balance must be zero before deletion.");
			}
			accountRepository.delete(account);
			deleted.add(accountNumber);
			return result(accountNumber, BulkItemStatus.DELETED, null);
		});
		if (!deleted.isEmpty()) {
			custAccXRefRepository.deleteAll(custAccXRefRepository.findByAccountNumberIn(deleted));
		}
		return BulkOperationResponse.of("DELETE", results);
	}

	private void validateBatch(List<?> items, String fieldName) {
		if (items == null || items.isEmpty()) {
			throw new BulkRequestException(HttpStatus.BAD_REQUEST, fieldName + " must contain at least one entry.");
		}
		if (items.size() > maxBatchSize) {
			throw new BulkRequestException(HttpStatus.BAD_REQUEST,
					fieldName + " exceeds the maximum batch size of " + maxBatchSize + ".");
		}
	}

	private void requireCustomer(Long customerNumber) {
		if (!customerRepository.findByCustomerNumber(customerNumber).isPresent()) {
			throw new BulkRequestException(HttpStatus.NOT_FOUND, "Customer Number " + customerNumber + " not found.");
		}
	}

	private static String validateNewAccount(AccountInformation accountInformation) {
		if (accountInformation == null) {
			return "Account entry is empty.";
		}
		if (accountInformation.getAccountNumber() == null || accountInformation.getAccountNumber() <= 0) {
			return "accountNumber must be a positive number.";
		}
		if (accountInformation.getBankInformation() == null
				|| accountInformation.getBankInformation().getBranchAddress() == null) {
			return "bankInformation with branchAddress is required.";
		}
		if (accountInformation.getAccountBalance() != null && accountInformation.getAccountBalance() < 0) {
			return "accountBalance cannot be negative.";
		}
		BankInformation bank = accountInformation.getBankInformation();
		AddressDetails address = bank.getBranchAddress();
		if (tooLong(accountInformation.getAccountType(), accountInformation.getAccountStatus(), bank.getBranchName(),
				address.getAddress1(), address.getAddress2(), address.getCity(), address.getState(),
				address.getZip(), address.getCountry())) {
			return "Text fields must be at most " + MAX_TEXT_LENGTH + " characters.";
		}
		return null;
	}

	private static boolean tooLong(String... values) {
		for (String value : values) {
			if (value != null && value.length() > MAX_TEXT_LENGTH) {
				return true;
			}
		}
		return false;
	}

	private Map<Long, Account> findAccounts(List<Long> accountNumbers, boolean forUpdate) {
		Set<Long> distinct = accountNumbers.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (distinct.isEmpty()) {
			return Map.of();
		}
		List<Account> found = forUpdate ? accountRepository.findForUpdateByAccountNumberIn(distinct)
				: accountRepository.findByAccountNumberIn(distinct);
		return found.stream()
				.collect(Collectors.toMap(Account::getAccountNumber, Function.identity(), (first, second) -> first));
	}

	private Set<Long> findOwnedAccountNumbers(Long customerNumber, List<Long> accountNumbers) {
		Set<Long> distinct = accountNumbers.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (distinct.isEmpty()) {
			return Set.of();
		}
		return custAccXRefRepository.findByCustomerNumberAndAccountNumberIn(customerNumber, distinct).stream()
				.map(CustomerAccountXRef::getAccountNumber)
				.collect(Collectors.toSet());
	}

	private static BulkItemResult checkManageable(Long accountNumber, Map<Long, Account> accounts, Set<Long> owned,
			Long customerNumber) {
		if (!accounts.containsKey(accountNumber)) {
			return result(accountNumber, BulkItemStatus.NOT_FOUND, "Account number not found.");
		}
		if (!owned.contains(accountNumber)) {
			return result(accountNumber, BulkItemStatus.NOT_OWNED,
					"Account does not belong to Customer Number " + customerNumber + ".");
		}
		return null;
	}

	private static List<BulkItemResult> processNumbers(List<Long> accountNumbers,
			Function<Long, BulkItemResult> processor) {
		Set<Long> seen = new HashSet<>();
		List<BulkItemResult> results = new ArrayList<>();
		for (Long accountNumber : accountNumbers) {
			if (accountNumber == null) {
				results.add(result(null, BulkItemStatus.INVALID, "accountNumber is required."));
			} else if (!seen.add(accountNumber)) {
				results.add(result(accountNumber, BulkItemStatus.DUPLICATE, "Account number repeated in request."));
			} else {
				results.add(processor.apply(accountNumber));
			}
		}
		return results;
	}

	private static BulkItemResult result(Long accountNumber, BulkItemStatus status, String message) {
		return BulkItemResult.builder().accountNumber(accountNumber).status(status).message(message).build();
	}
}

package com.coding.exercise.bankapp.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coding.exercise.bankapp.domain.AccountInformation;
import com.coding.exercise.bankapp.domain.AddressDetails;
import com.coding.exercise.bankapp.domain.BulkCustomerUpdateRequest;
import com.coding.exercise.bankapp.domain.BulkUpdateResponse;
import com.coding.exercise.bankapp.domain.BulkUpdateResult;
import com.coding.exercise.bankapp.domain.BulkUpdateStatus;
import com.coding.exercise.bankapp.domain.ContactDetails;
import com.coding.exercise.bankapp.domain.CustomerDetails;
import com.coding.exercise.bankapp.domain.TransactionDetails;
import com.coding.exercise.bankapp.domain.TransferDetails;
import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.model.Address;
import com.coding.exercise.bankapp.model.Contact;
import com.coding.exercise.bankapp.model.Customer;
import com.coding.exercise.bankapp.model.CustomerAccountXRef;
import com.coding.exercise.bankapp.model.Transaction;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerAccountXRefRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;
import com.coding.exercise.bankapp.repository.TransactionRepository;
import com.coding.exercise.bankapp.service.helper.BankingServiceHelper;

@Service
@Transactional
public class BankingServiceImpl implements BankingService {

	public static final int MAX_BULK_UPDATE_SIZE = 100;
	public static final int MAX_FIELD_LENGTH = 255;

	@Autowired
    private CustomerRepository customerRepository;
	@Autowired
    private AccountRepository accountRepository;
	@Autowired
    private TransactionRepository transactionRepository;
	@Autowired
    private CustomerAccountXRefRepository custAccXRefRepository;
    @Autowired
    private BankingServiceHelper bankingServiceHelper;

    public BankingServiceImpl(CustomerRepository repository) {
        this.customerRepository=repository;
    }
    
   
    public List<CustomerDetails> findAll() {
    	
    	List<CustomerDetails> allCustomerDetails = new ArrayList<>();

        Iterable<Customer> customerList = customerRepository.findAll();

        customerList.forEach(customer -> {
        	allCustomerDetails.add(bankingServiceHelper.convertToCustomerDomain(customer));
        });
        
        return allCustomerDetails;
    }

    /**
     * CREATE Customer
     * 
     * @param customerDetails
     * @return
     */
	public ResponseEntity<Object> addCustomer(CustomerDetails customerDetails) {
		
		Customer customer = bankingServiceHelper.convertToCustomerEntity(customerDetails);
		customer.setCreateDateTime(new Date());
		customerRepository.save(customer);
		
		return ResponseEntity.status(HttpStatus.CREATED).body("New Customer created successfully.");
	}

	/**
	 * READ Customer
	 * 
	 * @param customerNumber
	 * @return
	 */
    
	public CustomerDetails findByCustomerNumber(Long customerNumber) {
		
		Optional<Customer> customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

		if(customerEntityOpt.isPresent())
			return bankingServiceHelper.convertToCustomerDomain(customerEntityOpt.get());
		
		return null;
	}

	/**
	 * UPDATE Customer
	 * 
	 * @param customerDetails
	 * @param customerNumber
	 * @return
	 */
	public ResponseEntity<Object> updateCustomer(CustomerDetails customerDetails, Long customerNumber) {
		Optional<Customer> managedCustomerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);
		Customer unmanagedCustomerEntity = bankingServiceHelper.convertToCustomerEntity(customerDetails);
		if(managedCustomerEntityOpt.isPresent()) {
			Customer managedCustomerEntity = managedCustomerEntityOpt.get();
			
			if(Optional.ofNullable(unmanagedCustomerEntity.getContactDetails()).isPresent()) {
				
				Contact managedContact = managedCustomerEntity.getContactDetails();
				if(managedContact != null) {
					managedContact.setEmailId(unmanagedCustomerEntity.getContactDetails().getEmailId());
					managedContact.setHomePhone(unmanagedCustomerEntity.getContactDetails().getHomePhone());
					managedContact.setWorkPhone(unmanagedCustomerEntity.getContactDetails().getWorkPhone());
				} else
					managedCustomerEntity.setContactDetails(unmanagedCustomerEntity.getContactDetails());
			}
			
			if(Optional.ofNullable(unmanagedCustomerEntity.getCustomerAddress()).isPresent()) {
				
				Address managedAddress = managedCustomerEntity.getCustomerAddress();
				if(managedAddress != null) {
					managedAddress.setAddress1(unmanagedCustomerEntity.getCustomerAddress().getAddress1());
					managedAddress.setAddress2(unmanagedCustomerEntity.getCustomerAddress().getAddress2());
					managedAddress.setCity(unmanagedCustomerEntity.getCustomerAddress().getCity());
					managedAddress.setState(unmanagedCustomerEntity.getCustomerAddress().getState());
					managedAddress.setZip(unmanagedCustomerEntity.getCustomerAddress().getZip());
					managedAddress.setCountry(unmanagedCustomerEntity.getCustomerAddress().getCountry());
				} else
					managedCustomerEntity.setCustomerAddress(unmanagedCustomerEntity.getCustomerAddress());
			}
			
			managedCustomerEntity.setUpdateDateTime(new Date());
			managedCustomerEntity.setStatus(unmanagedCustomerEntity.getStatus());
			managedCustomerEntity.setFirstName(unmanagedCustomerEntity.getFirstName());
			managedCustomerEntity.setMiddleName(unmanagedCustomerEntity.getMiddleName());
			managedCustomerEntity.setLastName(unmanagedCustomerEntity.getLastName());
			managedCustomerEntity.setUpdateDateTime(new Date());
			
			customerRepository.save(managedCustomerEntity);
			
			return ResponseEntity.status(HttpStatus.OK).body("Success: Customer updated.");
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer Number " + customerNumber + " not found.");
		}
	}

	/**
	 * PATCH-style update of multiple customers.
	 *
	 * @param request customers and fields to update
	 * @return per-customer update results
	 */
	@Override
	@Transactional
	public ResponseEntity<Object> bulkUpdateCustomers(BulkCustomerUpdateRequest request) {
		if (request == null || request.getCustomers() == null || request.getCustomers().isEmpty()) {
			return ResponseEntity.badRequest().body("Bulk update request must contain at least one customer.");
		}
		if (request.getCustomers().size() > MAX_BULK_UPDATE_SIZE) {
			return ResponseEntity.badRequest().body("Bulk update request cannot exceed 100 customers.");
		}

		List<BulkUpdateResult> results = new ArrayList<>();
		Set<Long> seenCustomerNumbers = new HashSet<>();
		int updated = 0;

		for (CustomerDetails details : request.getCustomers()) {
			if (details == null || details.getCustomerNumber() == null) {
				results.add(BulkUpdateResult.builder()
						.customerNumber(details == null ? null : details.getCustomerNumber())
						.status(BulkUpdateStatus.INVALID)
						.message("customerNumber is required.")
						.build());
				continue;
			}

			Long customerNumber = details.getCustomerNumber();
			if (!seenCustomerNumbers.add(customerNumber)) {
				results.add(BulkUpdateResult.builder()
						.customerNumber(customerNumber)
						.status(BulkUpdateStatus.DUPLICATE)
						.message("Customer Number " + customerNumber + " appears more than once in the request.")
						.build());
				continue;
			}

			if (details.getFirstName() == null && details.getMiddleName() == null && details.getLastName() == null
					&& details.getStatus() == null && !hasContactFields(details.getContactDetails())
					&& !hasAddressFields(details.getCustomerAddress())) {
				results.add(BulkUpdateResult.builder()
						.customerNumber(customerNumber)
						.status(BulkUpdateStatus.INVALID)
						.message("No fields to update.")
						.build());
				continue;
			}

			String tooLongField = getTooLongField(details);
			if (tooLongField != null) {
				results.add(BulkUpdateResult.builder()
						.customerNumber(customerNumber)
						.status(BulkUpdateStatus.INVALID)
						.message(tooLongField + " exceeds 255 characters.")
						.build());
				continue;
			}

			Optional<Customer> customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);
			if (!customerEntityOpt.isPresent()) {
				results.add(BulkUpdateResult.builder()
						.customerNumber(customerNumber)
						.status(BulkUpdateStatus.NOT_FOUND)
						.message("Customer Number " + customerNumber + " not found.")
						.build());
				continue;
			}

			Customer customer = customerEntityOpt.get();
			if (details.getFirstName() != null) {
				customer.setFirstName(details.getFirstName());
			}
			if (details.getMiddleName() != null) {
				customer.setMiddleName(details.getMiddleName());
			}
			if (details.getLastName() != null) {
				customer.setLastName(details.getLastName());
			}
			if (details.getStatus() != null) {
				customer.setStatus(details.getStatus());
			}
			updateContact(customer, details.getContactDetails());
			updateAddress(customer, details.getCustomerAddress());
			customer.setUpdateDateTime(new Date());
			customerRepository.save(customer);

			updated++;
			results.add(BulkUpdateResult.builder()
					.customerNumber(customerNumber)
					.status(BulkUpdateStatus.UPDATED)
					.message("Customer updated.")
					.build());
		}

		return ResponseEntity.ok(BulkUpdateResponse.builder()
				.requested(request.getCustomers().size())
				.updated(updated)
				.failed(request.getCustomers().size() - updated)
				.results(results)
				.build());
	}

	private boolean hasContactFields(ContactDetails details) {
		return details != null && (details.getEmailId() != null || details.getHomePhone() != null
				|| details.getWorkPhone() != null);
	}

	private boolean hasAddressFields(AddressDetails details) {
		return details != null && (details.getAddress1() != null || details.getAddress2() != null
				|| details.getCity() != null || details.getState() != null || details.getZip() != null
				|| details.getCountry() != null);
	}

	private String getTooLongField(CustomerDetails details) {
		if (exceedsMaxFieldLength(details.getFirstName())) {
			return "firstName";
		}
		if (exceedsMaxFieldLength(details.getMiddleName())) {
			return "middleName";
		}
		if (exceedsMaxFieldLength(details.getLastName())) {
			return "lastName";
		}
		if (exceedsMaxFieldLength(details.getStatus())) {
			return "status";
		}

		ContactDetails contactDetails = details.getContactDetails();
		if (contactDetails != null) {
			if (exceedsMaxFieldLength(contactDetails.getEmailId())) {
				return "contactDetails.emailId";
			}
			if (exceedsMaxFieldLength(contactDetails.getHomePhone())) {
				return "contactDetails.homePhone";
			}
			if (exceedsMaxFieldLength(contactDetails.getWorkPhone())) {
				return "contactDetails.workPhone";
			}
		}

		AddressDetails addressDetails = details.getCustomerAddress();
		if (addressDetails != null) {
			if (exceedsMaxFieldLength(addressDetails.getAddress1())) {
				return "customerAddress.address1";
			}
			if (exceedsMaxFieldLength(addressDetails.getAddress2())) {
				return "customerAddress.address2";
			}
			if (exceedsMaxFieldLength(addressDetails.getCity())) {
				return "customerAddress.city";
			}
			if (exceedsMaxFieldLength(addressDetails.getState())) {
				return "customerAddress.state";
			}
			if (exceedsMaxFieldLength(addressDetails.getZip())) {
				return "customerAddress.zip";
			}
			if (exceedsMaxFieldLength(addressDetails.getCountry())) {
				return "customerAddress.country";
			}
		}

		return null;
	}

	private boolean exceedsMaxFieldLength(String value) {
		return value != null && value.length() > MAX_FIELD_LENGTH;
	}

	private void updateContact(Customer customer, ContactDetails details) {
		if (!hasContactFields(details)) {
			return;
		}

		Contact contact = customer.getContactDetails();
		if (contact == null) {
			customer.setContactDetails(Contact.builder()
					.emailId(details.getEmailId())
					.homePhone(details.getHomePhone())
					.workPhone(details.getWorkPhone())
					.build());
			return;
		}

		if (details.getEmailId() != null) {
			contact.setEmailId(details.getEmailId());
		}
		if (details.getHomePhone() != null) {
			contact.setHomePhone(details.getHomePhone());
		}
		if (details.getWorkPhone() != null) {
			contact.setWorkPhone(details.getWorkPhone());
		}
	}

	private void updateAddress(Customer customer, AddressDetails details) {
		if (!hasAddressFields(details)) {
			return;
		}

		Address address = customer.getCustomerAddress();
		if (address == null) {
			customer.setCustomerAddress(Address.builder()
					.address1(details.getAddress1())
					.address2(details.getAddress2())
					.city(details.getCity())
					.state(details.getState())
					.zip(details.getZip())
					.country(details.getCountry())
					.build());
			return;
		}

		if (details.getAddress1() != null) {
			address.setAddress1(details.getAddress1());
		}
		if (details.getAddress2() != null) {
			address.setAddress2(details.getAddress2());
		}
		if (details.getCity() != null) {
			address.setCity(details.getCity());
		}
		if (details.getState() != null) {
			address.setState(details.getState());
		}
		if (details.getZip() != null) {
			address.setZip(details.getZip());
		}
		if (details.getCountry() != null) {
			address.setCountry(details.getCountry());
		}
	}

	/**
	 * DELETE Customer
	 * 
	 * @param customerNumber
	 * @return
	 */
	public ResponseEntity<Object> deleteCustomer(Long customerNumber) {
		
		Optional<Customer> managedCustomerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

		if(managedCustomerEntityOpt.isPresent()) {
			Customer managedCustomerEntity = managedCustomerEntityOpt.get();
			customerRepository.delete(managedCustomerEntity);
			return ResponseEntity.status(HttpStatus.OK).body("Success: Customer deleted.");
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Customer does not exist.");
		}
		
		//TODO: Delete all customer entries from CustomerAccountXRef
	}

	/**
	 * Find Account
	 * 
	 * @param accountNumber
	 * @return
	 */
	public ResponseEntity<Object> findByAccountNumber(Long accountNumber) {
		
		Optional<Account> accountEntityOpt = accountRepository.findByAccountNumber(accountNumber);

		if(accountEntityOpt.isPresent()) {
			return ResponseEntity.status(HttpStatus.FOUND).body(bankingServiceHelper.convertToAccountDomain(accountEntityOpt.get()));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account Number " + accountNumber + " not found.");
		}
		
	}

	/**
	 * Create new account
	 * 
	 * @param accountInformation
	 * @param customerNumber 
	 * 
	 * @return
	 */
	public ResponseEntity<Object> addNewAccount(AccountInformation accountInformation, Long customerNumber) {
		
		Optional<Customer> customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

		if(customerEntityOpt.isPresent()) {
			accountRepository.save(bankingServiceHelper.convertToAccountEntity(accountInformation));
			
			// Add an entry to the CustomerAccountXRef
			custAccXRefRepository.save(CustomerAccountXRef.builder()
					.accountNumber(accountInformation.getAccountNumber())
					.customerNumber(customerNumber)
					.build());
			
		}

		return ResponseEntity.status(HttpStatus.CREATED).body("New Account created successfully.");
	}

	/**
	 * Transfer funds from one account to another for a specific customer
	 * 
	 * @param transferDetails
	 * @param customerNumber
	 * @return
	 */
	public ResponseEntity<Object> transferDetails(TransferDetails transferDetails, Long customerNumber) {
		
		List<Account> accountEntities = new ArrayList<>();
		Account fromAccountEntity = null;
		Account toAccountEntity = null;
		
		Optional<Customer> customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

		// If customer is present
		if(customerEntityOpt.isPresent()) {
			
			// get FROM ACCOUNT info
			Optional<Account> fromAccountEntityOpt = accountRepository.findByAccountNumber(transferDetails.getFromAccountNumber());
			if(fromAccountEntityOpt.isPresent()) {
				fromAccountEntity = fromAccountEntityOpt.get();
			}
			else {
			// if from request does not exist, 404 Bad Request
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("From Account Number " + transferDetails.getFromAccountNumber() + " not found.");
			}
			
			
			// get TO ACCOUNT info
			Optional<Account> toAccountEntityOpt = accountRepository.findByAccountNumber(transferDetails.getToAccountNumber());
			if(toAccountEntityOpt.isPresent()) {
				toAccountEntity = toAccountEntityOpt.get();
			}
			else {
			// if from request does not exist, 404 Bad Request
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("To Account Number " + transferDetails.getToAccountNumber() + " not found.");
			}

			
			// if not sufficient funds, return 400 Bad Request
			if(fromAccountEntity.getAccountBalance() < transferDetails.getTransferAmount()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient Funds.");
			}
			else {
				synchronized (this) {
					// update FROM ACCOUNT 
					fromAccountEntity.setAccountBalance(fromAccountEntity.getAccountBalance() - transferDetails.getTransferAmount());
					fromAccountEntity.setUpdateDateTime(new Date());
					accountEntities.add(fromAccountEntity);
					
					// update TO ACCOUNT
					toAccountEntity.setAccountBalance(toAccountEntity.getAccountBalance() + transferDetails.getTransferAmount());
					toAccountEntity.setUpdateDateTime(new Date());
					accountEntities.add(toAccountEntity);
					
					accountRepository.saveAll(accountEntities);
					
					// Create transaction for FROM Account
					Transaction fromTransaction = bankingServiceHelper.createTransaction(transferDetails, fromAccountEntity.getAccountNumber(), "DEBIT");
					transactionRepository.save(fromTransaction);
					
					// Create transaction for TO Account
					Transaction toTransaction = bankingServiceHelper.createTransaction(transferDetails, toAccountEntity.getAccountNumber(), "CREDIT");
					transactionRepository.save(toTransaction);
				}

				return ResponseEntity.status(HttpStatus.OK).body("Success: Amount transferred for Customer Number " + customerNumber);
			}
				
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer Number " + customerNumber + " not found.");
		}
		
	}

	/**
	 * Get all transactions for a specific account
	 * 
	 * @param accountNumber
	 * @return
	 */
	public List<TransactionDetails> findTransactionsByAccountNumber(Long accountNumber) {
		List<TransactionDetails> transactionDetails = new ArrayList<>();
		Optional<Account> accountEntityOpt = accountRepository.findByAccountNumber(accountNumber);
		if(accountEntityOpt.isPresent()) {
			Optional<List<Transaction>> transactionEntitiesOpt = transactionRepository.findByAccountNumber(accountNumber);
			if(transactionEntitiesOpt.isPresent()) {
				transactionEntitiesOpt.get().forEach(transaction -> {
					transactionDetails.add(bankingServiceHelper.convertToTransactionDomain(transaction));
				});
			}
		}
		
		return transactionDetails;
	}


}

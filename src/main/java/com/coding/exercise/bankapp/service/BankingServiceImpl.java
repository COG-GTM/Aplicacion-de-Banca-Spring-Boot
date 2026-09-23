package com.coding.exercise.bankapp.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coding.exercise.bankapp.domain.AccountInformation;
import com.coding.exercise.bankapp.domain.CustomerDetails;
import com.coding.exercise.bankapp.domain.TransactionDetails;
import com.coding.exercise.bankapp.domain.TransferDetails;
import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.model.CustomerAccountXRef;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerAccountXRefRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;
import com.coding.exercise.bankapp.repository.TransactionRepository;
import com.coding.exercise.bankapp.service.helper.BankingServiceHelper;

@Service
@Transactional
public class BankingServiceImpl implements BankingService {

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
    	
        return StreamSupport.stream(customerRepository.findAll().spliterator(), false)
        		.map(bankingServiceHelper::convertToCustomerDomain)
        		.toList();
    }

    /**
     * CREATE Customer
     * 
     * @param customerDetails
     * @return
     */
	public ResponseEntity<Object> addCustomer(CustomerDetails customerDetails) {
		
		var customer = bankingServiceHelper.convertToCustomerEntity(customerDetails);
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
		
		var customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

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
		var managedCustomerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);
		var unmanagedCustomerEntity = bankingServiceHelper.convertToCustomerEntity(customerDetails);
		if(managedCustomerEntityOpt.isPresent()) {
			var managedCustomerEntity = managedCustomerEntityOpt.get();
			
			if(unmanagedCustomerEntity.getContactDetails() != null) {
				
				var managedContact = managedCustomerEntity.getContactDetails();
				if(managedContact != null) {
					managedContact.setEmailId(unmanagedCustomerEntity.getContactDetails().getEmailId());
					managedContact.setHomePhone(unmanagedCustomerEntity.getContactDetails().getHomePhone());
					managedContact.setWorkPhone(unmanagedCustomerEntity.getContactDetails().getWorkPhone());
				} else
					managedCustomerEntity.setContactDetails(unmanagedCustomerEntity.getContactDetails());
			}
			
			if(unmanagedCustomerEntity.getCustomerAddress() != null) {
				
				var managedAddress = managedCustomerEntity.getCustomerAddress();
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
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer Number %d not found.".formatted(customerNumber));
		}
	}

	/**
	 * DELETE Customer
	 * 
	 * @param customerNumber
	 * @return
	 */
	public ResponseEntity<Object> deleteCustomer(Long customerNumber) {
		
		var managedCustomerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

		if(managedCustomerEntityOpt.isPresent()) {
			var managedCustomerEntity = managedCustomerEntityOpt.get();
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
		
		var accountEntityOpt = accountRepository.findByAccountNumber(accountNumber);

		if(accountEntityOpt.isPresent()) {
			return ResponseEntity.status(HttpStatus.FOUND).body(bankingServiceHelper.convertToAccountDomain(accountEntityOpt.get()));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account Number %d not found.".formatted(accountNumber));
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
		
		var customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

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
		
		var accountEntities = new ArrayList<Account>();
		Account fromAccountEntity = null;
		Account toAccountEntity = null;
		
		var customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

		// If customer is present
		if(customerEntityOpt.isPresent()) {
			
			// get FROM ACCOUNT info
			var fromAccountEntityOpt = accountRepository.findByAccountNumber(transferDetails.fromAccountNumber());
			if(fromAccountEntityOpt.isPresent()) {
				fromAccountEntity = fromAccountEntityOpt.get();
			}
			else {
			// if from request does not exist, 404 Bad Request
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("From Account Number %d not found.".formatted(transferDetails.fromAccountNumber()));
			}
			
			
			// get TO ACCOUNT info
			var toAccountEntityOpt = accountRepository.findByAccountNumber(transferDetails.toAccountNumber());
			if(toAccountEntityOpt.isPresent()) {
				toAccountEntity = toAccountEntityOpt.get();
			}
			else {
			// if from request does not exist, 404 Bad Request
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("To Account Number %d not found.".formatted(transferDetails.toAccountNumber()));
			}

			
			// if not sufficient funds, return 400 Bad Request
			if(fromAccountEntity.getAccountBalance() < transferDetails.transferAmount()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient Funds.");
			}
			else {
				synchronized (this) {
					// update FROM ACCOUNT 
					fromAccountEntity.setAccountBalance(fromAccountEntity.getAccountBalance() - transferDetails.transferAmount());
					fromAccountEntity.setUpdateDateTime(new Date());
					accountEntities.add(fromAccountEntity);
					
					// update TO ACCOUNT
					toAccountEntity.setAccountBalance(toAccountEntity.getAccountBalance() + transferDetails.transferAmount());
					toAccountEntity.setUpdateDateTime(new Date());
					accountEntities.add(toAccountEntity);
					
					accountRepository.saveAll(accountEntities);
					
					// Create transaction for FROM Account
					var fromTransaction = bankingServiceHelper.createTransaction(transferDetails, fromAccountEntity.getAccountNumber(), "DEBIT");
					transactionRepository.save(fromTransaction);
					
					// Create transaction for TO Account
					var toTransaction = bankingServiceHelper.createTransaction(transferDetails, toAccountEntity.getAccountNumber(), "CREDIT");
					transactionRepository.save(toTransaction);
				}

				return ResponseEntity.status(HttpStatus.OK).body("Success: Amount transferred for Customer Number %d".formatted(customerNumber));
			}
				
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer Number %d not found.".formatted(customerNumber));
		}
		
	}

	/**
	 * Get all transactions for a specific account
	 * 
	 * @param accountNumber
	 * @return
	 */
	public List<TransactionDetails> findTransactionsByAccountNumber(Long accountNumber) {
		var accountEntityOpt = accountRepository.findByAccountNumber(accountNumber);
		if(accountEntityOpt.isPresent()) {
			var transactionEntitiesOpt = transactionRepository.findByAccountNumber(accountNumber);
			return transactionEntitiesOpt
					.map(transactions -> transactions.stream()
							.map(bankingServiceHelper::convertToTransactionDomain)
							.toList())
					.orElse(List.of());
		}
		
		return List.of();
	}


}

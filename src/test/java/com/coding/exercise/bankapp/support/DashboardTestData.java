package com.coding.exercise.bankapp.support;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coding.exercise.bankapp.model.Account;
import com.coding.exercise.bankapp.model.Address;
import com.coding.exercise.bankapp.model.BankInfo;
import com.coding.exercise.bankapp.model.Contact;
import com.coding.exercise.bankapp.model.Customer;
import com.coding.exercise.bankapp.model.CustomerAccountXRef;
import com.coding.exercise.bankapp.model.Transaction;
import com.coding.exercise.bankapp.repository.AccountRepository;
import com.coding.exercise.bankapp.repository.CustomerAccountXRefRepository;
import com.coding.exercise.bankapp.repository.CustomerRepository;
import com.coding.exercise.bankapp.repository.TransactionRepository;

/**
 * Resets the H2 database to the dashboard demo data used by the integration and
 * end-to-end tests, mirroring what DashboardDataInitializer creates outside of
 * the "test" profile.
 */
@Component
@Profile("test")
public class DashboardTestData {

    public static final Long CUSTOMER_NUMBER = 1001L;
    public static final Long CHECKING_ACCOUNT = 5001L;
    public static final Long SAVINGS_ACCOUNT = 5002L;
    public static final Double CHECKING_BALANCE = 15750.50;
    public static final Double SAVINGS_BALANCE = 42300.00;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerAccountXRefRepository custAccXRefRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public void reset() {
        entityManager.createQuery("delete from Transaction").executeUpdate();
        entityManager.createQuery("delete from CustomerAccountXRef").executeUpdate();
        entityManager.createQuery("delete from Account").executeUpdate();
        entityManager.createQuery("delete from Customer").executeUpdate();

        customerRepository.save(Customer.builder()
                .firstName("Carlos")
                .lastName("Rodriguez")
                .middleName("A")
                .customerNumber(CUSTOMER_NUMBER)
                .status("Active")
                .customerAddress(address())
                .contactDetails(Contact.builder()
                        .emailId("carlos.rodriguez@bbva.mx")
                        .homePhone("+52-55-1234-5678")
                        .workPhone("+52-55-8765-4321")
                        .build())
                .createDateTime(new Date())
                .build());

        accountRepository.save(account(CHECKING_ACCOUNT, "CHECKING", CHECKING_BALANCE));
        accountRepository.save(account(SAVINGS_ACCOUNT, "SAVINGS", SAVINGS_BALANCE));

        custAccXRefRepository.save(CustomerAccountXRef.builder()
                .accountNumber(CHECKING_ACCOUNT).customerNumber(CUSTOMER_NUMBER).build());
        custAccXRefRepository.save(CustomerAccountXRef.builder()
                .accountNumber(SAVINGS_ACCOUNT).customerNumber(CUSTOMER_NUMBER).build());

        transactionRepository.save(transaction(CHECKING_ACCOUNT, "DEBIT", 2500.00));
        transactionRepository.save(transaction(SAVINGS_ACCOUNT, "CREDIT", 2500.00));
        transactionRepository.save(transaction(CHECKING_ACCOUNT, "CREDIT", 500.00));
    }

    public static Account account(Long accountNumber, String accountType, Double balance) {
        return Account.builder()
                .accountNumber(accountNumber)
                .accountType(accountType)
                .accountStatus("Active")
                .accountBalance(balance)
                .bankInformation(BankInfo.builder()
                        .branchCode(1234)
                        .branchName("BBVA Torre Reforma")
                        .routingNumber(123456789)
                        .branchAddress(address())
                        .build())
                .createDateTime(new Date())
                .build();
    }

    public static Transaction transaction(Long accountNumber, String txType, Double amount) {
        return Transaction.builder()
                .accountNumber(accountNumber)
                .txDateTime(new Date())
                .txType(txType)
                .txAmount(amount)
                .build();
    }

    private static Address address() {
        return Address.builder()
                .address1("Av. Paseo de la Reforma 510")
                .address2("Col. Juarez")
                .city("Mexico City")
                .state("CDMX")
                .zip("06600")
                .country("Mexico")
                .build();
    }
}

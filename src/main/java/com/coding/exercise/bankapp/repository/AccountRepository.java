package com.coding.exercise.bankapp.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.coding.exercise.bankapp.model.Account;

@Repository
public interface AccountRepository extends CrudRepository<Account, String> {

	Optional<Account> findByAccountNumber(Long accountNumber);

	List<Account> findByAccountNumberIn(Collection<Long> accountNumbers);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<Account> findForUpdateByAccountNumberIn(Collection<Long> accountNumbers);
}

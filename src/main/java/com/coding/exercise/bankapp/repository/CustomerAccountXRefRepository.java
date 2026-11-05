package com.coding.exercise.bankapp.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.coding.exercise.bankapp.model.CustomerAccountXRef;

@Repository
public interface CustomerAccountXRefRepository extends CrudRepository<CustomerAccountXRef, String> {

	Optional<CustomerAccountXRef> findByCustomerNumberAndAccountNumber(Long customerNumber, Long accountNumber);

	void deleteByCustomerNumber(Long customerNumber);
}

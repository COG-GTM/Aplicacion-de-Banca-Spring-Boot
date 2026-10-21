package com.coding.exercise.bankapp.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.coding.exercise.bankapp.model.CustomerAccountXRef;

@Repository
public interface CustomerAccountXRefRepository extends CrudRepository<CustomerAccountXRef, String> {

	List<CustomerAccountXRef> findByCustomerNumberAndAccountNumberIn(Long customerNumber, Collection<Long> accountNumbers);

	List<CustomerAccountXRef> findByAccountNumberIn(Collection<Long> accountNumbers);
}

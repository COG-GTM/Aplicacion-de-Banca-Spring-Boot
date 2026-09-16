package com.coding.exercise.bankapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coding.exercise.bankapp.domain.CustomerDetails;
import com.coding.exercise.bankapp.service.BankingServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("customers")
@Tag(name = "Customer REST endpoints")
public class CustomerController {

	@Autowired
	private BankingServiceImpl bankingService;

	@GetMapping(path = "/all")
	@Operation(summary = "Find all customers", description = "Gets details of all the customers")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })

	public List<CustomerDetails> getAllCustomers() {

		return bankingService.findAll();
	}

	@PostMapping(path = "/add")
	@Operation(summary = "Add a Customer", description = "Add customer and create an account")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })

	public ResponseEntity<Object> addCustomer(@RequestBody CustomerDetails customer) {

		return bankingService.addCustomer(customer);
	}

	@GetMapping(path = "/{customerNumber}")
	@Operation(summary = "Get customer details", description = "Get Customer details by customer number.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })

	public CustomerDetails getCustomer(@PathVariable Long customerNumber) {

		return bankingService.findByCustomerNumber(customerNumber);
	}

	@PutMapping(path = "/{customerNumber}")
	@Operation(summary = "Update customer", description = "Update customer and any other account information associated with him.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })

	public ResponseEntity<Object> updateCustomer(@RequestBody CustomerDetails customerDetails,
			@PathVariable Long customerNumber) {

		return bankingService.updateCustomer(customerDetails, customerNumber);
	}

	@DeleteMapping(path = "/{customerNumber}")
	@Operation(summary = "Delete customer and related accounts", description = "Delete customer and all accounts associated with him.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })

	public ResponseEntity<Object> deleteCustomer(@PathVariable Long customerNumber) {

		return bankingService.deleteCustomer(customerNumber);
	}

}

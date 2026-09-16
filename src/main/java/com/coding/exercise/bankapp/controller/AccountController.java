package com.coding.exercise.bankapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coding.exercise.bankapp.domain.AccountInformation;
import com.coding.exercise.bankapp.domain.TransactionDetails;
import com.coding.exercise.bankapp.domain.TransferDetails;
import com.coding.exercise.bankapp.service.BankingServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("accounts")
@Tag(name = "Accounts and Transactions REST endpoints")
public class AccountController {

	@Autowired
	private BankingServiceImpl bankingService;

	@GetMapping(path = "/{accountNumber}")
	@Operation(summary = "Get account details", description = "Find account details by account number")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })

	public ResponseEntity<Object> getByAccountNumber(@PathVariable Long accountNumber) {

		return bankingService.findByAccountNumber(accountNumber);
	}

	@PostMapping(path = "/add/{customerNumber}")
	@Operation(summary = "Add a new account", description = "Create an new account for existing customer.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })

	public ResponseEntity<Object> addNewAccount(@RequestBody AccountInformation accountInformation,
			@PathVariable Long customerNumber) {

		return bankingService.addNewAccount(accountInformation, customerNumber);
	}

	@PutMapping(path = "/transfer/{customerNumber}")
	@Operation(summary = "Transfer funds between accounts", description = "Transfer funds between accounts.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })

	public ResponseEntity<Object> transferDetails(@RequestBody TransferDetails transferDetails,
			@PathVariable Long customerNumber) {

		return bankingService.transferDetails(transferDetails, customerNumber);
	}

	@GetMapping(path = "/transactions/{accountNumber}")
	@Operation(summary = "Get all transactions", description = "Get all Transactions by account number")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })

	public List<TransactionDetails> getTransactionByAccountNumber(@PathVariable Long accountNumber) {

		return bankingService.findTransactionsByAccountNumber(accountNumber);
	}
}

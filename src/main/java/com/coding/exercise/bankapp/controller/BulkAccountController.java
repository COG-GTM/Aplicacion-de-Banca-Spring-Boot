package com.coding.exercise.bankapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coding.exercise.bankapp.domain.bulk.BulkAccountNumbersRequest;
import com.coding.exercise.bankapp.domain.bulk.BulkAccountRequest;
import com.coding.exercise.bankapp.domain.bulk.BulkAccountStatusRequest;
import com.coding.exercise.bankapp.domain.bulk.BulkOperationResponse;
import com.coding.exercise.bankapp.service.BulkAccountService;
import com.coding.exercise.bankapp.service.BulkRequestException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("accounts/bulk")
@Tag(name = "Bulk account number management REST endpoints")
public class BulkAccountController {

	private final BulkAccountService bulkAccountService;

	public BulkAccountController(BulkAccountService bulkAccountService) {
		this.bulkAccountService = bulkAccountService;
	}

	@PostMapping(path = "/add/{customerNumber}")
	@Operation(summary = "Add accounts in bulk", description = "Create many accounts for an existing customer. Each account number gets its own result (CREATED, DUPLICATE or INVALID).")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Batch processed; see per-item results"),
			@ApiResponse(responseCode = "400", description = "Empty or oversized batch"),
			@ApiResponse(responseCode = "404", description = "Customer not found") })

	public BulkOperationResponse addAccounts(@RequestBody BulkAccountRequest request,
			@PathVariable Long customerNumber) {

		return bulkAccountService.addAccounts(customerNumber, request);
	}

	@PostMapping(path = "/lookup")
	@Operation(summary = "Look up accounts in bulk", description = "Get account details for many account numbers (FOUND or NOT_FOUND per account number).")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Batch processed; see per-item results"),
			@ApiResponse(responseCode = "400", description = "Empty or oversized batch") })

	public BulkOperationResponse lookupAccounts(@RequestBody BulkAccountNumbersRequest request) {

		return bulkAccountService.lookupAccounts(request);
	}

	@PutMapping(path = "/status/{customerNumber}")
	@Operation(summary = "Update account status in bulk", description = "Set the status of many accounts owned by a customer (UPDATED, NOT_FOUND or NOT_OWNED per account number).")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Batch processed; see per-item results"),
			@ApiResponse(responseCode = "400", description = "Empty or oversized batch, or missing accountStatus"),
			@ApiResponse(responseCode = "404", description = "Customer not found") })

	public BulkOperationResponse updateAccountStatus(@RequestBody BulkAccountStatusRequest request,
			@PathVariable Long customerNumber) {

		return bulkAccountService.updateAccountStatus(customerNumber, request);
	}

	@PostMapping(path = "/delete/{customerNumber}")
	@Operation(summary = "Delete accounts in bulk", description = "Delete many zero-balance accounts owned by a customer (DELETED, REJECTED, NOT_FOUND or NOT_OWNED per account number).")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Batch processed; see per-item results"),
			@ApiResponse(responseCode = "400", description = "Empty or oversized batch"),
			@ApiResponse(responseCode = "404", description = "Customer not found") })

	public BulkOperationResponse deleteAccounts(@RequestBody BulkAccountNumbersRequest request,
			@PathVariable Long customerNumber) {

		return bulkAccountService.deleteAccounts(customerNumber, request);
	}

	@ExceptionHandler(BulkRequestException.class)
	public ResponseEntity<Object> handleBulkRequestException(BulkRequestException ex) {

		return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
	}
}

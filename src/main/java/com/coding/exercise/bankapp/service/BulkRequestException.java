package com.coding.exercise.bankapp.service;

import org.springframework.http.HttpStatus;

public class BulkRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HttpStatus status;

	public BulkRequestException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}

package com.coding.exercise.bankapp.domain;

public record TransferDetails(Long fromAccountNumber, Long toAccountNumber, Double transferAmount) {}

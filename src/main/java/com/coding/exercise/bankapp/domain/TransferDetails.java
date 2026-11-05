package com.coding.exercise.bankapp.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransferDetails {

	public static final int MAX_INTEGER_DIGITS = 13;

	public static final int MAX_FRACTION_DIGITS = 2;

	private Long fromAccountNumber;
	
	private Long toAccountNumber;
	
	private BigDecimal transferAmount;
}

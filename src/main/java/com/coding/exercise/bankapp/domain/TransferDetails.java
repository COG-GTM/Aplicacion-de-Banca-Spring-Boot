package com.coding.exercise.bankapp.domain;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

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

	@NotNull
	private Long fromAccountNumber;
	
	@NotNull
	private Long toAccountNumber;
	
	@NotNull
	@DecimalMin(value = "0.01")
	@Digits(integer = TransferDetails.MAX_INTEGER_DIGITS, fraction = TransferDetails.MAX_FRACTION_DIGITS)
	private BigDecimal transferAmount;
}

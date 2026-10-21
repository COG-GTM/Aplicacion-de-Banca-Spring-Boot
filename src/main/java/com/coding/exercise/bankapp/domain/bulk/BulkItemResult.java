package com.coding.exercise.bankapp.domain.bulk;

import com.coding.exercise.bankapp.domain.AccountInformation;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkItemResult {

	private Long accountNumber;

	private BulkItemStatus status;

	private String message;

	private AccountInformation account;
}

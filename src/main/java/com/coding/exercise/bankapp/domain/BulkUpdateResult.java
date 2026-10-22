package com.coding.exercise.bankapp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateResult {

	private Long customerNumber;
	private BulkUpdateStatus status;
	private String message;
}

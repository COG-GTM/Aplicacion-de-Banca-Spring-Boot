package com.coding.exercise.bankapp.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateResponse {

	private int requested;
	private int updated;
	private int failed;
	private List<BulkUpdateResult> results;
}

package com.coding.exercise.bankapp.domain.bulk;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkOperationResponse {

	private String operation;

	private int requested;

	private int succeeded;

	private int failed;

	private List<BulkItemResult> results;

	public static BulkOperationResponse of(String operation, List<BulkItemResult> results) {
		int succeeded = (int) results.stream().filter(r -> r.getStatus().isSuccess()).count();
		return new BulkOperationResponse(operation, results.size(), succeeded, results.size() - succeeded, results);
	}
}

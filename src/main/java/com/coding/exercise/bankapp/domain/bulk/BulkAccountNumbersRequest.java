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
public class BulkAccountNumbersRequest {

	private List<Long> accountNumbers;
}

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
public class BulkAccountStatusRequest {

	private List<Long> accountNumbers;

	private String accountStatus;
}

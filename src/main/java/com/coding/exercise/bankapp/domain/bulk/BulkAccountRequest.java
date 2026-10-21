package com.coding.exercise.bankapp.domain.bulk;

import java.util.List;

import com.coding.exercise.bankapp.domain.AccountInformation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkAccountRequest {

	private List<AccountInformation> accounts;
}

package com.coding.exercise.bankapp.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CustomerAccountXRef {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="CUST_ACC_XREF_ID")
	@Type(type = "uuid-char")
	private UUID id;
	
	@Column(unique = true)
	private Long accountNumber;
	
	private Long customerNumber;
	
}

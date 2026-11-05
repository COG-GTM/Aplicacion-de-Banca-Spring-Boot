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

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class Contact {

	@Id
	@Type(type = "uuid-char")
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="CONTACT_ID")
	private UUID id;
	
	private String emailId;
	
	private String homePhone;
	
	private String workPhone;

}

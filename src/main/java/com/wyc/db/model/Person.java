package com.wyc.db.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Person {
	
	public static enum Role {
		DRIVER, ADMIN
	}
	
	@Id
	private Integer id;
	
	private String carNumber;

	private String userName;
	
	private String phoneNumber;

	private String firstName;
	
	private String lastName;

	private String languageCode;
	
	@Enumerated(EnumType.STRING)
	private Role role;
}

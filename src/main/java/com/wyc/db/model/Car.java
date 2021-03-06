package com.wyc.db.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Car {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	private String number;
	
	private String ownerPhoneNumber;

	private String ownerFirstName;

	private String ownerLastName;

	private Integer ownerUserId;
	
	@ManyToOne
	private Person reporter;
	
	private Date creationDate;
}

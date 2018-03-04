package com.wyc.db.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMessage {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	private Date creationDate;
	
	private Integer senderId;
	
	private String text;
	
	private String data;

	private String contactFirstName;

	private String contactLastName;

	private String contactPhoneNumber;

	private Integer contactUserId;
	
	private Float longitude;
	
	private Float latitude;
}

package com.wyc.db.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.wyc.Location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMessage implements Location{

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	private Date creationDate;
	
	private String senderId;
	
	private String text;
	
	@Column(columnDefinition="TEXT")
	public String body;
	
	private String data;

	private String contactFirstName;

	private String contactLastName;

	private String contactPhoneNumber;

	private String contactUserId;
	
	private Float longitude;
	
	private Float latitude;
	
	private String messageId;
}

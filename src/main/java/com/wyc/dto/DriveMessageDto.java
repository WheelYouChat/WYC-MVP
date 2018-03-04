package com.wyc.dto;

import java.util.Date;

import lombok.Data;

@Data
public class DriveMessageDto {

	private String carNumberTo;
	
	private Date creationDate;

	private String message;
	
	private PersonDto from;
	
	private float longitude;

	private float latitude;
	
	private String locationTitle;
	
	private DriveMessageTypeDto messageType;
	
	/**
	 * Ответ на это сообщение
	 */
	private DriveMessageDto answer;
}

package com.wyc.db.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.wyc.chat.HasTitle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DriveMessage {

	@Getter
	@AllArgsConstructor
	public static enum DriveMessageType implements HasTitle {
		PODREZALI("Вы меня подрезали"),
		USTUPILI("Спасибо, что уступили дорогу"),
		NOT_USTUPILI("Вы мне не уступили"),
		WRONG_PARKING("Ваша машина неправильно припаркована");
		
		private final String title;
	}
	

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	private Person from;
	
	@ManyToOne
	private Person to;

	private String carNumberTo;
	
	private String message;

	@Enumerated(EnumType.STRING)
	private DriveMessageType messageType;
	
	private Date sentDate;
	
	private Date deliveredDate;
	
	private String deliveryException;
	
	private Float longitude;

	private Float latitude;
	
	private String locationTitle;
}

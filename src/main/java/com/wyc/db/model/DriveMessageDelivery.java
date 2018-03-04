package com.wyc.db.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
@Table(uniqueConstraints=@UniqueConstraint(columnNames="code"))
public class DriveMessageDelivery {
	
	public static enum DeliveryType {
		SMS,
		TELEGRAM
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	private DriveMessage driveMessage;
	
	@ManyToOne
	private Person to;

	private Date deliveredDate;
	
	private String deliveryException;
	
	private Integer sentMessageId;
	
	@Enumerated(EnumType.STRING)
	private DeliveryType deliveryType;

	private String code;
	
	private String phoneNumber;
}

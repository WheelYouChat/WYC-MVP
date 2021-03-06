package com.wyc.db.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.wyc.sms.sender.SMSDeliveryStatusProvider.DeliveryStatus;

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
		TELEGRAM,
		VIBER
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	private DriveMessage driveMessage;
	
	@ManyToOne
	private Person to;

	private Date deliveredDate;

	private Date sentDate;
	
	private String smsId;
	
	@Column(columnDefinition="TEXT")
	private String smsSentResponse;
	
	private String deliveryException;
	
	private String sentMessageId;
	
	@Enumerated(EnumType.STRING)
	private DeliveryType deliveryType;
	
	@Enumerated(EnumType.STRING)
	private DeliveryStatus deliveryStatus;
	
	private boolean completed;

	private String code;
	
	private String phoneNumber;
}

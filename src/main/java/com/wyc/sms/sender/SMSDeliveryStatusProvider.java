package com.wyc.sms.sender;

import com.wyc.db.model.DriveMessageDelivery;

import lombok.AllArgsConstructor;
import lombok.Getter;

public interface SMSDeliveryStatusProvider {
	
	@AllArgsConstructor
	@Getter
	public enum DeliveryStatus {
		MODERATION(false), DELIVERED(true), ERROR(true), IN_PROGRESS(false), DECLINED(true);
		private final boolean completed;
	}
	
	DeliveryStatus getStatus(DriveMessageDelivery delivery);
}

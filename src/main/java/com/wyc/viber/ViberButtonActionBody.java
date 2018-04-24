package com.wyc.viber;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class ViberButtonActionBody {
	public static enum Event {
		clicked,
	}
	
	private Event event; 
	private String beanName;
	private String methodName;
	private String[] params;
	private Long replyToMessageId;
}

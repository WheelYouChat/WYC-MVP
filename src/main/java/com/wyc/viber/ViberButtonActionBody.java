package com.wyc.viber;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ViberButtonActionBody {
	public static enum Event {
		clicked,
	}
	
	private Event event; 
	private String beanName;
	private String methodName;
}

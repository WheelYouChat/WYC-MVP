package com.wyc.controller;

import lombok.Getter;

@Getter
public class UIException extends RuntimeException {
	private final String fieldName;

	public UIException(String message, String fieldName) {
		super(message);
		this.fieldName = fieldName;
	}

}

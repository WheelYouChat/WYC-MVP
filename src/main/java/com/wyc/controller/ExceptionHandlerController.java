package com.wyc.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public abstract class ExceptionHandlerController {

	@RequiredArgsConstructor
	@Getter
	public static class WYCError{
		private final String message;
		private final String exception;
		private final String fieldName;
	}
	
	/*
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody WYCError handleIllegalArgumentException(IllegalArgumentException exception) {
		return new WYCError(exception.getMessage(), exception.getClass().getSimpleName(), null);
	}
	
	@ExceptionHandler(NullPointerException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody WYCError handleNullPointerException(NullPointerException exception) {
		return new WYCError(exception.getMessage(), exception.getClass().getSimpleName(), null);
	}
	*/
	
	@ExceptionHandler(UIException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody WYCError handleUIException(UIException exception) {
		return new WYCError(exception.getMessage(), exception.getClass().getSimpleName(), exception.getFieldName());
	}
}

package com.wyc.chat.validator;

import com.wyc.chat.BotParamValidator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class BaseValidator<T> implements BotParamValidator<T>{
	private final Class<T> validationClass;
	
}

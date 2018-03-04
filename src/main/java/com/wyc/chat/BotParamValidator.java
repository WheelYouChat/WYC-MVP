package com.wyc.chat;

import java.util.List;

public interface BotParamValidator<T> {
	List<String> validate(T s);
	Class<T> getValidationClass();
}

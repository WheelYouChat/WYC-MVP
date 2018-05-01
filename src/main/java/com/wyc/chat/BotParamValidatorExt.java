package com.wyc.chat;

import java.util.List;

import com.wyc.db.model.Person;

public interface BotParamValidatorExt<T> extends BotParamValidator<T>{
	List<String> validate(T s, Person p);
}

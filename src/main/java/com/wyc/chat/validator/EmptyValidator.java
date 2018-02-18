package com.wyc.chat.validator;

import java.util.Collections;
import java.util.List;

import com.wyc.chat.BotParamValidator;

public class EmptyValidator implements BotParamValidator {

	@Override
	public List<String> validate(String s) {
		return Collections.emptyList();
	}

}

package com.wyc.chat.validator;

import java.util.Collections;
import java.util.List;

import com.wyc.chat.BotParamValidator;

import jersey.repackaged.com.google.common.collect.Lists;

public class NicknameValidator implements BotParamValidator {

	@Override
	public List<String> validate(String s) {
		s = s.trim();
		if(s.length() < 3) {
			return Lists.newArrayList("Слишком короткое имя (должно быть минимум 3 символов)");
		}
		if(s.length() > 50) {
			return Lists.newArrayList("Слишком длинное имя (должно быть максимум 50 символов)");
		}
		return Collections.emptyList();
	}

}

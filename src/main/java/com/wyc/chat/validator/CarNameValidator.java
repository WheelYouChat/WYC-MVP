package com.wyc.chat.validator;

import java.util.Collections;
import java.util.List;

import jersey.repackaged.com.google.common.collect.Lists;

public class CarNameValidator extends BaseValidator<String> {

	public CarNameValidator() {
		super(String.class);
	}

	@Override
	public List<String> validate(String s) {
		s = s.trim();
		if(s.length() < 3) {
			return Lists.newArrayList("Слишком короткое имя (должно быть минимум 3 символов)");
		}
		if(s.length() > 100) {
			return Lists.newArrayList("Слишком длинное имя (должно быть максимум 100 символов)");
		}
		String[] parts = s.split("[ \n\r]+");
		if(parts.length < 2) {
			return Lists.newArrayList("Используйте как минимум два слова (например цвет машины и марку)");
		}
		return Collections.emptyList();
	}

}

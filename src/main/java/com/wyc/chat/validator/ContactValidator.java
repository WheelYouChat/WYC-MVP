package com.wyc.chat.validator;

import java.util.Collections;
import java.util.List;

import org.telegram.telegrambots.api.objects.Message;

import jersey.repackaged.com.google.common.collect.Lists;

public class ContactValidator extends BaseValidator<Message>{

	public ContactValidator() {
		super(Message.class);
	}

	@Override
	public List<String> validate(Message message) {
		boolean hasData = false;
		if(message.getText() != null) {
			String phone = message.getText().trim();
			int digitCount = digitsNumber(phone);
			if(digitCount < 10) {
				return Lists.newArrayList("В номере телефона должно быть не менее 10 цифр.");
			}
			if(digitCount > 15) {
				return Lists.newArrayList("В номере телефона не должно быть более 15 цифр.");
			}
			if(phone.length() > 30) {
				return Lists.newArrayList("Слишком длинный номер телефона.");
			}
			hasData = true;
		}
		
		if(message.getContact() != null) {
			hasData = true;
		}
		
		if(!hasData) {
			return Lists.newArrayList("Не могу распознать контакт.\nПришлите номер телефона или контакт из адресной книги.");
		}
		return Collections.emptyList();
	}

	protected int digitsNumber(String phone) {
		int count = 0;
		for(int i = 0; i < phone.length(); i++) {
			char c = phone.charAt(i);
			if(Character.isDigit(c)) {
				count++;
			}
		}
		return count;
	}

}

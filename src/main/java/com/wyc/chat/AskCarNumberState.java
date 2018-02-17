package com.wyc.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.api.objects.Update;

import com.wyc.db.model.Person;
import com.wyc.db.repository.PersonRepository;

public class AskCarNumberState extends AskStringState {
	
	@Autowired
	private PersonRepository personRepository;

	@Override
	protected void doAction(Update update) {
		/*
		Long chatId = update.getMessage().getChatId();
		Person person = personRepository.findOne(chatId);
		person.setCarNumber(update.getMessage().getText());
		personRepository.save(person);
		//*/
	}
}

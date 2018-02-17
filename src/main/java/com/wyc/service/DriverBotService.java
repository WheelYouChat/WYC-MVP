package com.wyc.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wyc.annotation.BotMethod;
import com.wyc.annotation.BotMethodParam;
import com.wyc.annotation.BotService;
import com.wyc.annotation.BotUser;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.Person;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.PersonRepository;

@BotService(title="Водитель", roles = Person.Role.DRIVER)
@Service
public class DriverBotService {
	
	@Autowired
	private PersonRepository personRepository;
	
	@Autowired
	private DriveMessageRepository driveMessageRepository;
	
	@BotMethod(title="Сообщить номер вашего автомобиля")
	public void setNumber(@BotMethodParam(title="Номер вашего автомобиля") String newNumber, @BotUser String currentUserId) {
		Person person = personRepository.findOne(Integer.parseInt(currentUserId));
		person.setCarNumber(newNumber);
		personRepository.save(person);
	}
	
	/*
	@BotMethod(title="Сообщить модель авто")
	public void setCarModel(@BotMethodParam(title="Марка автомобиля") String newCar, @BotUser String currentUserId) {
		Person person = personRepository.findOne(Integer.parseInt(currentUserId));
		// person.setCarNumber(newNumber);
		
	}
	*/
	
	@BotMethod(title="Послать сообщение другому водителю.", successMessage="Ваше сообщение будет отослано.")
	public void sendMessage(@BotMethodParam(title="Номер автомобиля (кому хотите послать сообщение)") String number, 
			@BotMethodParam(title="Введите сообщение") String message,
			@BotUser String currentUserId) {
		Person person = personRepository.findOne(Integer.parseInt(currentUserId));
		// person.setCarNumber(newNumber);
		DriveMessage driveMessage = DriveMessage.builder()
				.from(person)
				.numberTo(number)
				.sentDate(new Date())
				.message(message)
				.build();
		driveMessageRepository.save(driveMessage);
		
	}
	
	@BotMethod(title="lihachat.ru", url="http://www.lihachat.ru")
	public void webSite() {
		
	}
}

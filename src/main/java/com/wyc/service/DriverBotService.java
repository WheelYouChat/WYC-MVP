package com.wyc.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.objects.Location;

import com.wyc.annotation.BotMethod;
import com.wyc.annotation.BotMethodParam;
import com.wyc.annotation.BotService;
import com.wyc.annotation.BotUser;
import com.wyc.chat.validator.PlateValidator;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
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
	public void setNumber(@BotMethodParam(title="Номер вашего автомобиля", validators=PlateValidator.class) String newNumber, @BotUser String currentUserId) {
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
	public void sendMessage(@BotMethodParam(title="Номер автомобиля (кому хотите послать сообщение)", validators=PlateValidator.class) String number, 
			// @BotMethodParam(title="Введите сообщение") String message,
			@BotMethodParam(title="Введите сообщение") DriveMessageType messageType,
			@BotMethodParam(title="Укажите место где произошло") Location location,
			@BotUser Integer currentUserId) {
		Person person = personRepository.findOne(currentUserId);
		// person.setCarNumber(newNumber);
		DriveMessage driveMessage = DriveMessage.builder()
				.from(person)
				.carNumberTo(number)
				.sentDate(new Date())
				.messageType(messageType)
				.message(messageType.getTitle())
				.longitude(location.getLongitude())
				.latitude(location.getLatitude())
				.build();
		driveMessageRepository.save(driveMessage);
		
	}
	
	@BotMethod(title="lihachat.ru", url="http://www.lihachat.ru")
	public void webSite() {
		
	}
	
	@BotMethod(title="Просмотреть последние сообщения.")
	public String[] getLastMessages(@BotUser Integer currentUserId) {
		List<DriveMessage> messages = driveMessageRepository.findByToId(currentUserId);
		List<String> lst = messages.stream().map(DriveMessage::getMessage).collect(Collectors.toList());
		if(lst.isEmpty()) {
			return new String[]{"У вас нет принятых сообщений."};
		}
		return lst.toArray(new String[lst.size()]);
	}
}

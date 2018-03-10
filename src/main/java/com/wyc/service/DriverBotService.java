package com.wyc.service;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.objects.Contact;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Message;

import com.wyc.annotation.BotMethod;
import com.wyc.annotation.BotMethodParam;
import com.wyc.annotation.BotService;
import com.wyc.annotation.BotUser;
import com.wyc.annotation.ReplyBotMethod;
import com.wyc.annotation.ReplyMessageId;
import com.wyc.chat.validator.CarNameValidator;
import com.wyc.chat.validator.ContactValidator;
import com.wyc.chat.validator.NicknameValidator;
import com.wyc.chat.validator.PlateValidator;
import com.wyc.db.model.Car;
import com.wyc.db.model.Car.CarBuilder;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.model.Person;
import com.wyc.db.repository.CarRepository;
import com.wyc.db.repository.DriveMessageDeliveryRepository;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.PersonRepository;

import lombok.extern.slf4j.Slf4j;

@BotService(title="Водитель", roles = Person.Role.DRIVER)
@Service
@Slf4j
public class DriverBotService {
	
	@Autowired
	private PersonRepository personRepository;
	
	@Autowired
	private DriveMessageRepository driveMessageRepository;
	
	@Autowired
	private DriveMessageDeliveryRepository driveMessageDeliveryRepository;
	
	@Autowired
	private CarRepository carRepository;
	
	@BotMethod(title="Заполнить профиль (номер, ник, авто)")
	public String setNumber(
			@BotMethodParam(title="Номер вашего автомобиля в формате А123АА99", validators=PlateValidator.class) String newNumber, 
			@BotMethodParam(title="Ваш ник (например Лихач)", validators=NicknameValidator.class) String newNickname, 
			@BotMethodParam(title="Описание вашего автомобиля (например Красная Феррари)", validators=CarNameValidator.class) String newCarName, 
			@BotUser String currentUserId) {
		Person person = personRepository.findByTelegramId(Integer.parseInt(currentUserId)).orElseThrow(() -> new ResourceNotFoundException("Can not find person by telegram id = '" + currentUserId + "'"));
		newNumber = PlateValidator.processNumber(newNumber);
		person.setCarNumber(newNumber);
		person.setNickname(newNickname);
		person.setCarName(newCarName);
		personRepository.save(person);
		return "Спасибо за регистрацию.\nТеперь все будут видеть вас как '" + person.getUserDesc() + "'"; 
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
		Person person = personRepository.findByTelegramId(currentUserId).orElseThrow(() -> new ResourceNotFoundException("Can not find person by telegram id = '" + currentUserId + "'"));
		// person.setCarNumber(newNumber);
		number = PlateValidator.processNumber(number);
		DriveMessage driveMessage = DriveMessage.builder()
				.from(person)
				.carNumberTo(number)
				.creationDate(new Date())
				.messageType(messageType)
				.message(messageType.getTitle())
				.smsMessage(messageType.getSms())
				.longitude(location.getLongitude())
				.latitude(location.getLatitude())
				.build();
		driveMessageRepository.save(driveMessage);
		
	}
	
	@BotMethod(title="lihachat.ru", url="http://www.lihachat.ru")
	public void webSite() {
		
	}
	
	/*
	@BotMethod(title="Просмотреть последние сообщения.")
	public String[] getLastMessages(@BotUser Integer currentUserId) {
		List<DriveMessage> messages = driveMessageRepository.findByToId(currentUserId);
		List<String> lst = messages.stream().map(DriveMessage::getMessage).collect(Collectors.toList());
		if(lst.isEmpty()) {
			return new String[]{"У вас нет принятых сообщений."};
		}
		return lst.toArray(new String[lst.size()]);
	}
	*/
	
	
	@BotMethod(title="Сообщить номер телефона владельца.")
	public String reportCarPhone(
			@BotMethodParam(title="Номер автомобиля", validators=PlateValidator.class) String number,
			@BotMethodParam(title="Контакт владельца (введите номер телефона или пришлите контакт из адресной книги)", validators=ContactValidator.class) Message contactInfo,
			@BotUser Integer currentUserId) {
		
		Person reporter = Person.builder().telegramId(currentUserId).build();
		CarBuilder builder = Car.builder()
			.number(number)
			.creationDate(new Date())
			.reporter(reporter);
		
		if(contactInfo.getText() != null) {
			builder = builder.ownerPhoneNumber(contactInfo.getText());
		}
		
		if(contactInfo.getContact() != null) {
			Contact contact = contactInfo.getContact();
			builder = builder
					.ownerFirstName(contact.getFirstName())
					.ownerLastName(contact.getLastName())
					.ownerPhoneNumber(contact.getPhoneNumber())
					.ownerUserId(contact.getUserID())
					;
		}
		Car car = builder.build();
		carRepository.save(car);
		return "Спасибо за предоставленную информацию";
	}
	
	@ReplyBotMethod
	public void reply(DriveMessageType messageType, @ReplyMessageId Integer replyMessageId, @BotUser String currentUserId) {
		log.info("Reply");
		Person sender = personRepository.findByTelegramId(Integer.parseInt(currentUserId)).orElseThrow(() -> new ResourceNotFoundException("Can not find person by telegram id = '" + currentUserId + "'"));
		Optional<DriveMessageDelivery> messageDeliveryOpt = driveMessageDeliveryRepository.findBySentMessageId(replyMessageId);
		if(messageDeliveryOpt.isPresent()) {
			DriveMessageDelivery replyMessageDelivery = messageDeliveryOpt.get();
			DriveMessage replyMessage = replyMessageDelivery.getDriveMessage();
			DriveMessage driveMessage = DriveMessage.builder()
					.from(sender)
					.carNumberTo(replyMessage.getFrom().getCarNumber())
					.creationDate(new Date())
					.messageType(messageType)
					.message(messageType.getTitle())
					.smsMessage(messageType.getSms())
					.repliedTo(replyMessage)
					.build();
			driveMessageRepository.save(driveMessage);
			
		}
	}
}

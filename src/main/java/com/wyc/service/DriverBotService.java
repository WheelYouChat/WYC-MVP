package com.wyc.service;

import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.objects.Contact;
import org.telegram.telegrambots.api.objects.Message;

import com.wyc.Location;
import com.wyc.annotation.BotMethod;
import com.wyc.annotation.BotMethodParam;
import com.wyc.annotation.BotService;
import com.wyc.annotation.BotUser;
import com.wyc.annotation.ReplyBotMethod;
import com.wyc.annotation.ReplyMessageId;
import com.wyc.chat.validator.CarNameValidator;
import com.wyc.chat.validator.ContactValidator;
import com.wyc.chat.validator.NicknameValidator;
import com.wyc.chat.validator.PlateOrPedestrianValidator;
import com.wyc.chat.validator.PlateValidator;
import com.wyc.db.model.Car;
import com.wyc.db.model.Car.CarBuilder;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
import com.wyc.db.model.FeedbackMessage.State;
import com.wyc.db.model.Person.Role;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.model.FeedbackMessage;
import com.wyc.db.model.Person;
import com.wyc.db.repository.CarRepository;
import com.wyc.db.repository.DriveMessageDeliveryRepository;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.FeedbackMessageRepository;
import com.wyc.db.repository.PersonRepository;
import com.wyc.exception.ResourceNotFoundException;

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
	private FeedbackMessageRepository feedbackMessageRepository;
	
	@Autowired
	private DriveMessageDeliveryRepository driveMessageDeliveryRepository;
	
	@Autowired
	private CarRepository carRepository;
	
	@BotMethod(title="Заполнить профиль как водитель (номер, ник, авто)", mainMenu=true)
	public String setNumber(
			@BotMethodParam(title="Номер вашего автомобиля в формате 'А123АА99'.", validators=PlateValidator.class) String newNumber, 
			@BotMethodParam(title="Ваш ник (например 'Лихач Вася')", validators=NicknameValidator.class) String newNickname, 
			@BotMethodParam(title="Описание вашего автомобиля (цвет и марка, например 'Красная Феррари')", validators=CarNameValidator.class) String newCarName, 
			@BotUser String currentUserId) {
		
		Person person;
		try {
			person = personRepository.findByTelegramId(Integer.parseInt(currentUserId)).orElseThrow(() -> new ResourceNotFoundException("Can not find person by telegram id = '" + currentUserId + "'"));
		} catch (NumberFormatException e) {
			person = personRepository.findByViberId(currentUserId).orElseThrow(() -> new ResourceNotFoundException("Can not find person by telegram id = '" + currentUserId + "'"));
		}
		newNumber = PlateValidator.processNumber(newNumber);
		person.setCarNumber(newNumber);
		person.setNickname(newNickname);
		person.setCarName(newCarName);
		if(person.getRole() == null || person.getRole() != Role.ADMIN) {
			person.setRole(Role.DRIVER);
		}
		personRepository.save(person);
		return "Спасибо за регистрацию.\nТеперь все будут видеть вас как '" + person.getUserDesc() + "'"; 
	}

	@BotMethod(title="Заполнить профиль как пешеход", mainMenu=true)
	public String setNickname(
			@BotMethodParam(title="Ваш ник (например 'Вася в кепке')", validators=NicknameValidator.class) String newNickname, 
			@BotUser String currentUserId) {
		
		Person person;
		try {
			person = personRepository.findByTelegramId(Integer.parseInt(currentUserId)).orElseThrow(() -> new ResourceNotFoundException("Can not find person by telegram id = '" + currentUserId + "'"));
		} catch (NumberFormatException e) {
			person = personRepository.findByViberId(currentUserId).orElseThrow(() -> new ResourceNotFoundException("Can not find person by telegram id = '" + currentUserId + "'"));
		}
		person.setNickname(newNickname);
		if(person.getRole() == null || person.getRole() != Role.ADMIN) {
			person.setRole(Role.PEDESTRIAN);
		}
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
	
	@BotMethod(title="Послать сообщение другому водителю", successMessage="Ваше сообщение будет отослано.", order = -1000, mainMenu=true)
	public void sendMessage(
			@BotMethodParam(title="Номер автомобиля (кому хотите послать сообщение)", validators=PlateValidator.class) String number, 
			@BotMethodParam(title="Выберите сообщение") DriveMessageType messageType,
			
			// @BotMethodParam(title="Введите сообщение") String message,
			@BotMethodParam(title="Укажите место где произошло") Location location,
			@BotUser String currentUserId) {
		Person person = personRepository.findByViberId(currentUserId).orElseThrow(() -> new ResourceNotFoundException("Can not find person by telegram id = '" + currentUserId + "'"));
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
	
	@BotMethod(title="lihachat.ru", url="https://www.lihachat.ru", order = 1000, mainMenu=true)
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
	
	
	// @BotMethod(title="Сообщить номер телефона владельца", mainMenu=true)
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
					.ownerUserId(contact.getUserID());
		}
		Car car = builder.build();
		carRepository.save(car);
		return "Спасибо за предоставленную информацию";
	}
	
	@BotMethod(title="Возврат в главное меню", backToMainMenu=true, cols=6)
	public void backToMainMenu() {
	}

	
	@ReplyBotMethod
	public void reply(DriveMessageType messageType, @ReplyMessageId Long replyMessageId, @BotUser String currentUserId) {
		log.info("Reply");
		Person sender = personRepository.findByViberId(currentUserId).orElseThrow(() -> new ResourceNotFoundException("Can not find person by telegram id = '" + currentUserId + "'"));
		DriveMessage replyMessage = driveMessageRepository.findOne(replyMessageId);
		DriveMessage driveMessage = DriveMessage.builder()
				.from(sender)
				.carNumberTo(replyMessage.getFrom().getCarNumber())
				.creationDate(new Date())
				.messageType(messageType)
				.message(messageType.getTitle())
				.smsMessage(messageType.getSms())
				.repliedTo(replyMessage)
				.to(replyMessage == null ? null : replyMessage.getFrom())
				.build();
		driveMessageRepository.save(driveMessage);
	}

	@BotMethod(title="Показать профиль пользователя", mainMenu=true, order=-100)
	public String showProfile(@BotUser String currentUserId) {
		log.info("showProfile");
		StringBuilder sb = new StringBuilder();
		Person user = personRepository.findByViberId(currentUserId).orElseThrow(() -> new ResourceNotFoundException("Can not find person by viber id = '" + currentUserId + "'"));
		sb.append("Ник: " + ObjectUtils.firstNonNull(user.getNickname(), "<НД>") + "\n");
		sb.append("Машина: " + ObjectUtils.firstNonNull(user.getCarName(), "<НД>") + "\n");
		sb.append("Номер: " + ObjectUtils.firstNonNull(user.getCarNumber(), "<НД>") + "\n");
		sb.append("Роль: " + user.getRole().getTitle());
		
		return sb.toString();
	}
			
	@BotMethod(title="Написать в техподдержку", mainMenu=true, order=10)
	public String giveFeedback(@BotMethodParam(title="Введите текст вашего сообщения для нас") String feedbackMessageText, @BotUser String currentUserId) {
		log.info("giveFeedback");
		Person user = personRepository.findByViberId(currentUserId).orElseThrow(() -> new ResourceNotFoundException("Can not find person by viber id = '" + currentUserId + "'"));
		FeedbackMessage feedbackMessage = FeedbackMessage.builder()
				.creationDate(new Date())
				.reporter(user)
				.state(State.NEW)
				.message(feedbackMessageText)
				.build();
		feedbackMessageRepository.save(feedbackMessage);
		return "Спасибо за ваше обращение";
	}
}

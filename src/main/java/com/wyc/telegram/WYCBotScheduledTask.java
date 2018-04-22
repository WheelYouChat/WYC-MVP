package com.wyc.telegram;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import com.wyc.MethodDesc;
import com.wyc.WYCConfig;
import com.wyc.annotation.BotService;
import com.wyc.annotation.ReplyBotMethod;
import com.wyc.chat.validator.PlateValidator;
import com.wyc.db.model.AVICar;
import com.wyc.db.model.Car;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.model.DriveMessageDelivery.DeliveryType;
import com.wyc.db.model.Person;
import com.wyc.db.model.PersonContext;
import com.wyc.db.repository.AVICarRepository;
import com.wyc.db.repository.CarRepository;
import com.wyc.db.repository.ContextItemRepository;
import com.wyc.db.repository.DriveMessageDeliveryRepository;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.IncomingMessageRepository;
import com.wyc.db.repository.PersonContextRepository;
import com.wyc.db.repository.PersonRepository;
import com.wyc.service.AnswerService;
import com.wyc.service.CodeGenerator;
import com.wyc.service.MenuService;
import com.wyc.service.PersonService;
import com.wyc.service.SMSMessageGenerator;
import com.wyc.sms.sender.SMSDeliveryStatusProvider;
import com.wyc.sms.sender.SMSDeliveryStatusProvider.DeliveryStatus;
import com.wyc.sms.sender.SMSSender;
import com.wyc.viber.ViberApi;
import com.wyc.viber.ViberBot;
import com.wyc.viber.ViberButton;
import com.wyc.viber.ViberButton.ViberButtonActionType;
import com.wyc.viber.ViberKeyBoard;
import com.wyc.viber.ViberKeyBoard.ViberKeyBoardType;
import com.wyc.viber.ViberMessage.ViberMessageType;
import com.wyc.viber.ViberMessage;
import com.wyc.viber.ViberSentMessage;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WYCBotScheduledTask {

	@Autowired
	private WYCConfig wycConfig;
	
	@Autowired
	private PersonContextRepository personContextRepository;
	
	@Autowired
	private ContextItemRepository contextItemRepository;

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
	private AnswerService answerService;

	@Autowired
	private DriveMessageRepository driveMessageRepository;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private ViberBot viberBot;
	
	@Autowired
	private DriveMessageDeliveryRepository driveMessageDeliveryRepository;
	
	@Autowired
	private IncomingMessageRepository incomingMessageRepository;
	
	@Autowired
	private CarRepository carRepository;
	
	@Autowired
	private AVICarRepository aviCarRepository;
	
	@Autowired
	private SMSSender smsSender;
	
	private WYCBot wycBot;
	
	@Autowired
	private SMSMessageGenerator smsMessageGenerator;
	
	@Autowired
	private CodeGenerator codeGenerator;

	@Autowired
	private MenuService menuService;

	@Autowired
	private ViberApi viberApi;

	@PostConstruct
	public void initBot() throws TelegramApiRequestException {
		log.info("Registering WYC Bot...");
		ApiContextInitializer.init();
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
		if(wycConfig.getBot() != null && !wycConfig.getBot().isDisabled()) {
			wycBot = wycConfig.getBot();
			wycBot.setPersonRepository(personRepository);
			wycBot.setPersonContextRepository(personContextRepository);
			wycBot.setContextItemRepository(contextItemRepository);
			wycBot.setApplicationContext(applicationContext);
			wycBot.setAnswerService(answerService);
			wycBot.setDriveMessageRepository(driveMessageRepository);
			wycBot.setDriveMessageDeliveryRepository(driveMessageDeliveryRepository);
			wycBot.setIncomingMessageRepository(incomingMessageRepository);
			wycBot.setCarRepository(carRepository);
			wycBot.setMenuService(menuService);
			telegramBotsApi.registerBot(wycBot);
			log.info("WYC Bot is registered");
		}
	}

	private volatile boolean deliveryStarted = false;
	
	/**
	 * Рассылаем недоставленные сообщения
	 */
	@Scheduled(fixedRate=10000)
	public void deliveryMessages() {
		if(!deliveryStarted) {
			try {
				deliveryStarted = true;
				if(wycConfig.getBot() != null && !wycConfig.getBot().isDisabled()) {
					wycBot.deliveryMessages();
					deliverySmsMessages();
					checkSMSDeliveryStatus();
				}
				deliveryViberMessages();
			} finally {
				deliveryStarted = false;
			}
		}
	}


	/**
	 * Сбрасываем контексты тех пользователей, которые "повисли" в каком-либо контексте.
	 */
	@Scheduled(fixedRate=12000) // 2 min
	protected void clearViberContexts() {
		Date lastActivityDate = new Date(System.currentTimeMillis() - 1000 * 60 * 1);
		List<PersonContext> contexts = personContextRepository.findByLastActivityDateLessThan(lastActivityDate);
		for(PersonContext ctx : contexts) {
			Person p = ctx.getPerson();
			if(p.getViberId() != null) {
				ViberKeyBoard menu = viberApi.createMainMenu();
				try {
					viberApi.sendMessage(p.getViberId(), "Начните сначала", menu, ViberMessageType.text, "ЛихаЧат");
				} catch (IOException e) {
					log.error("Error sending message to " + p, e);
				}
			}
			answerService.clearContext(p.getId());
		}
	}

	protected void deliveryViberMessages() {
		Iterable<DriveMessage> messages = driveMessageRepository.findByDeliveredIsFalseOrderByIdDesc();
		for(DriveMessage message : messages) {
			// if(message.getTo() != null && message.getTo().getTelegramId() != null && (message.getLongitude() == null || message.getLocationTitle() != null)) { // Странное условие- чтобы to было заполнено, закомментировал 31.03.18
			if(message.getLongitude() == null || message.getLocationTitle() != null) {
				List<Person> persons = new ArrayList();
				String carNumber = message.getCarNumberTo();
				
				if(carNumber != null) {
					// Сообщение послали на номер авто
					carNumber = carNumber.toUpperCase();
					carNumber = carNumber.replaceAll("[ ]+", "");
					// List<Person> persons = personRepository.findByCarNumber(carNumber);
					
					List<Car> cars = carRepository.findByNumber(carNumber);
					Set<Integer> personIds = cars.stream().filter(car -> {return car.getOwnerUserId() != null;}).map(Car::getOwnerUserId).collect(Collectors.toSet());
					persons = personRepository.findByCarNumberOrIdIn(carNumber, personIds);
				} else if(message.getTo() != null){
					// Сообщение послали напрямую в Viber (ответили)
					persons.add(message.getTo());
				}
				for(Person person : persons) {
					if(person.getViberId() != null && !personService.hasActiveContext(person)) {
						ViberMessage sendMessage = new ViberMessage();
						String text = createMessageText(message);
						sendMessage.setText(text);
						
						ViberKeyBoard replyMarkup = createReplyButtons(message.getMessageType());
						sendMessage.setKeyboard(replyMarkup);
	
						DriveMessageDelivery messageDelivery = DriveMessageDelivery
								.builder()
								.deliveredDate(new Date())
								.deliveryType(DeliveryType.VIBER)
								.to(person)
								.driveMessage(message)
								.build();

						try {
							ViberSentMessage sentMessage = viberApi.sendMessage(person.getViberId(), sendMessage, message.getFrom().getUserDesc());
							messageDelivery.setSentMessageId(sentMessage.getMessage_token());

							message.setDelivered(true);
							driveMessageRepository.save(message);
							// TODO redesing it - Нужно проставлять флаг delivered при получении асинхронного ответа
						} catch (IOException e) {
							log.error("Error sending", e);
						}
						
						/*
						try {
							messageDelivery.setSentMessageId(sentMessage.getMessageId());
							driveMessageRepository.save(message);
						} catch (TelegramApiException e) {
							log.error("Error delivering message", e);
							messageDelivery.setDeliveryException(e.toString());
						}
						//*/
						driveMessageDeliveryRepository.save(messageDelivery);
					}
				}
			}
		}
	}
	

	protected ViberKeyBoard createReplyButtons(DriveMessageType messageType) {
		
		List<MethodDesc> replyMethods = getReplyMethods();
		String dataPrefix = "";
		List<ViberButton> buttons = new ArrayList<>();
		if(replyMethods.size() > 0) {
			MethodDesc methodDesc = replyMethods.get(0);
			dataPrefix = methodDesc.getBeanName() + "." + methodDesc.getMethod().getName() + ".";
		}
		if(messageType != null && messageType.getAnswers().length > 0) {
			for(DriveMessageType answer : messageType.getAnswers()) {
				ViberButton button = ViberButton
						.builder()
						.ActionBody(dataPrefix + answer.getName())
						.ActionType(ViberButtonActionType.reply)
						.Text(answer.getTitle())
						.BgColor(answer.getColor())
						.build();
				
				buttons.add(button);
			}
		}
		ViberKeyBoard res = ViberKeyBoard
				.builder()
				.Type(ViberKeyBoardType.keyboard)
				.Buttons(buttons.toArray(new ViberButton[buttons.size()]))
				.build();
		return res;
	}
	
	protected List<MethodDesc> getReplyMethods() {
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(BotService.class);
		List<MethodDesc> res = new ArrayList<>();

		InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
		inlineKeyboardMarkup.setKeyboard(keyboard);

		log.debug("Start response. Scan services");
		for(String name : beans.keySet()) {
			Object bean = beans.get(name);
			log.debug("  bot service " + name + " = " + bean);
			Class<? extends Object> cls = bean.getClass();
			for(Method m : cls.getMethods()) {
				if(m.isAnnotationPresent(ReplyBotMethod.class)) {
					res.add(MethodDesc.builder()
							.bean(bean)
							.beanName(name)
							.method(m)
							.build());
				}
			}
		}
		return res;
		
	}

	protected String createMessageText(DriveMessage message) {
		String res = "";
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
		if(message.getFrom() != null) {
			res = res + "Вам пишет " + message.getFrom().getUserDesc() + "\n" + message.getMessage() + 
					(message.getLocationTitle() == null ? "" : "\nМесто: " + message.getLocationTitle());
		} else {
			// Это ответ от незарегистрированного пользователя
			res = res + "Вам пишет " + message.getRepliedTo().getCarNumberTo() + "\n*" + message.getMessage() +"*";
		}
		// res = res + (message.getLocationTitle() == null ? "" : "\nМесто: " + message.getLocationTitle()) + "\n";

		if(message.getRepliedTo() != null) {
			res = res + "\n\nв ответ на ваше сообщение от " + sdf.format(message.getRepliedTo().getCreationDate()) +  
					"\n_" +message.getRepliedTo().getMessage() + "_" +
					"\nМесто : " + message.getRepliedTo().getLocationTitle() +
					"";
			
		}
		
		return res;
	}
	

	protected void deliverySmsMessages() {
		Iterable<DriveMessage> messages = driveMessageRepository.findByDeliveredIsFalseOrderByIdDesc();
		for(DriveMessage message : messages) {
			if(message.getTo() == null && message.getCarNumberTo() != null && message.getLocationTitle() != null) {
				// Адресат не подключен к системе
				String carNumberTo = PlateValidator.processNumber(message.getCarNumberTo());
				List<AVICar> aviCars = aviCarRepository.findByNumberOrderByCreationDate(carNumberTo);
				if(!aviCars.isEmpty()) {
					// TODO - проверить насколько старый запрос и обновить данные из AVInfo если прошло много времени
					AVICar aviCar = aviCars.get(0);
					String phoneNumber = aviCar.getPhoneNumber();
					if(isCorrect(phoneNumber)) {
						// Проверяем последнюю доставку на этот номер
						List<DriveMessageDelivery> prevDeliveries = driveMessageDeliveryRepository.findByPhoneNumberOrderBySentDate(phoneNumber);
						Date lastSentDate = null;
						for(DriveMessageDelivery prevDelivery : prevDeliveries) {
							if(prevDelivery.getDeliveryException() == null) {
								lastSentDate = prevDelivery.getSentDate();
							}
						}
						
						if(lastSentDate == null || (System.currentTimeMillis() - lastSentDate.getTime()) > 90 * 24 * 60 * 60 * 1000) { 
							// Если доставки не было, либо прошло более 90 суток с последней доставки (3 месяца)
							// TODO - переделать на проверку deliveredDate - даты доставки
							DriveMessageDelivery messageDelivery;
							try {
								String code = codeGenerator.generateUniqueString();
								String text = smsMessageGenerator.generateSMSMessage(message.getMessageType(), message.getCreationDate(), message.getLocationTitle(), code);
								String smsSentResponse = smsSender.sendMessage(phoneNumber, text);
								
								messageDelivery = DriveMessageDelivery.builder()
										.sentDate(new Date())
										.code(code)
										.smsSentResponse(smsSentResponse)
										.driveMessage(message)
										.deliveryType(DeliveryType.SMS)
										.phoneNumber(phoneNumber)
										.build();
								message.setDelivered(true);
								message.setSmsMessage(text);
								driveMessageRepository.save(message);
							} catch (Exception e) {
								log.error("Error sending message to " + phoneNumber, e);
								messageDelivery = DriveMessageDelivery.builder()
										.sentDate(new Date())
										.deliveryException(e.getMessage())
										.deliveryType(DeliveryType.SMS)
										.phoneNumber(phoneNumber)
										.build();
							}
							driveMessageDeliveryRepository.save(messageDelivery);
						}
					}
				}
			}
		}
		
	}
	
	// Проверка статусов СМС сообщений (доставлено или не доставлено)
	protected void checkSMSDeliveryStatus() {
		if(smsSender instanceof SMSDeliveryStatusProvider) {
			SMSDeliveryStatusProvider statusProvider = (SMSDeliveryStatusProvider) smsSender;
			List<DriveMessageDelivery> messages = driveMessageDeliveryRepository.findByCompletedAndDeliveryType(false, DeliveryType.SMS);
			for(DriveMessageDelivery message : messages) {
				DeliveryStatus status = statusProvider.getStatus(message);
				message.setDeliveryStatus(status);
				message.setCompleted(status.isCompleted());
				if(status == DeliveryStatus.DELIVERED) {
					message.setDeliveredDate(new Date());
				}
				driveMessageDeliveryRepository.save(message);
			}
		}
	}

	/**
	 * Проверка правильности номера телефона
	 * @param phoneNumber
	 * @return
	 */
	private boolean isCorrect(String phoneNumber) {
		return phoneNumber != null && phoneNumber.trim().length() > 9;
	}
}

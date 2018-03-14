package com.wyc.telegram;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import com.wyc.WYCConfig;
import com.wyc.chat.validator.PlateValidator;
import com.wyc.db.model.AVICar;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.model.DriveMessageDelivery.DeliveryType;
import com.wyc.db.repository.AVICarRepository;
import com.wyc.db.repository.CarRepository;
import com.wyc.db.repository.ContextItemRepository;
import com.wyc.db.repository.DriveMessageDeliveryRepository;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.IncomingMessageRepository;
import com.wyc.db.repository.PersonContextRepository;
import com.wyc.db.repository.PersonRepository;
import com.wyc.service.CodeGenerator;
import com.wyc.service.SMSMessageGenerator;
import com.wyc.sms.sender.SMSDeliveryStatusProvider;
import com.wyc.sms.sender.SMSDeliveryStatusProvider.DeliveryStatus;
import com.wyc.sms.sender.SMSSender;

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
	private DriveMessageRepository driveMessageRepository;
	
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
			wycBot.setDriveMessageRepository(driveMessageRepository);
			wycBot.setDriveMessageDeliveryRepository(driveMessageDeliveryRepository);
			wycBot.setIncomingMessageRepository(incomingMessageRepository);
			wycBot.setCarRepository(carRepository);
			telegramBotsApi.registerBot(wycBot);
			log.info("WYC Bot is registered");
		}
	}

	@Scheduled(fixedRate=10000)
	public void deliveryMessages() {
		if(wycConfig.getBot() != null && !wycConfig.getBot().isDisabled()) {
			wycBot.deliveryMessages();
			deliverySmsMessages();
			checkSMSDeliveryStatus();
		}
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

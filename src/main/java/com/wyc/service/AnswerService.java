package com.wyc.service;

import java.awt.TrayIcon.MessageType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wyc.controller.UIException;
import com.wyc.chat.validator.PlateValidator;
import com.wyc.controller.AnswerRestController.Dialog;
import com.wyc.controller.AnswerRestController.DialogPerson;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
import com.wyc.db.model.Person.Role;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.model.Person;
import com.wyc.db.repository.DriveMessageDeliveryRepository;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.PersonRepository;

@Service
public class AnswerService {

	@Autowired
	private DriveMessageDeliveryRepository driveMessageDeliveryRepository;
	
	@Autowired
	private DriveMessageRepository driveMessageRepository;
	
	@Autowired
	private PersonRepository personRepository;
	
	public Optional<DriveMessageDelivery> getDriveMessageDelivery(String code) {
		return driveMessageDeliveryRepository.findByCode(code);
	}
	
	public DriveMessage answerTo(DriveMessage message, DriveMessageType messageType) {
		List<DriveMessage> answers = getAnswers(message.getId());
		if(answers.size() > 0) {
			throw new UIException("На сообщение уже ответили", "");
		}
		
		DriveMessageType answerType = Arrays.stream(message.getMessageType().getAnswers()).filter(t -> t == messageType).findFirst().orElseThrow(() -> new UIException("Неправильный вариант ответа", "answer"));
		
		DriveMessage answer = DriveMessage.builder()
			.creationDate(new Date())
			.delivered(false)
			.message(answerType.getTitle())
			.smsMessage(answerType.getSms())
			.messageType(answerType)
			.repliedTo(message)
			.to(message.getFrom())
			.build();
		driveMessageRepository.save(answer);
		return answer;
		
	}

	public List<DriveMessage> getAnswers(Long messageId) {
		return driveMessageRepository.findByRepliedToId(messageId);
	}
	
	public List<Dialog> getFakeDialogs() {
		List<Dialog> res = new ArrayList<>();
		
		List<DriveMessage> messages = new ArrayList(driveMessageRepository.findByFromRole(Role.FAKE));
		Collections.sort(messages, new Comparator<DriveMessage>() {

			@Override
			public int compare(DriveMessage m1, DriveMessage m2) {
				return -m1.getCreationDate().compareTo(m2.getCreationDate());
			}
		});
		
		for(int i = 0; i < messages.size() - 1; i++) {
			DriveMessage m1 = messages.get(i);
			DriveMessage m2 = messages.get(i + 1);
			if(m1.getMessageType() == DriveMessageType.CUT_OFF && m2.getMessageType() == DriveMessageType.CUT_OFF_SORRY) {
				DriveMessage tmp = m1;
				m1 = m2;
				m2 = tmp;
			}
			if(m1.getCreationDate().equals(m2.getCreationDate()) && m1.getMessageType() == DriveMessageType.CUT_OFF_SORRY && m2.getMessageType() == DriveMessageType.CUT_OFF) {
				// Нашли пару сообщений
				Dialog dialog = new Dialog();
				dialog.setLocationTitle(m1.getLocationTitle());
				dialog.setLongitude(m1.getLongitude());
				dialog.setLatitude(m1.getLatitude());
				dialog.setCreationDate(m1.getCreationDate());
				
				DialogPerson cutter =  new DialogPerson();
				cutter.setNumber(m1.getFrom().getCarNumber());
				cutter.setBrand(m1.getFrom().getCarName());
				DialogPerson cutted =  new DialogPerson();
				cutted.setNumber(m2.getFrom().getCarNumber());
				cutted.setBrand(m2.getFrom().getCarName());
				dialog.setCutter(cutter);
				dialog.setCutted(cutted);
				res.add(dialog );
			}
		}
		return res;
	}

	public void makeFakeDialog(Dialog dialog) {
		Person cutter = createFakePerson(dialog.getCutter());
		Person cutted = createFakePerson(dialog.getCutted());
		personRepository.save(cutter); 
		personRepository.save(cutted); 

		Date creationDate = new Date();
		
		DriveMessage toCutter = DriveMessage.builder()
				.carNumberTo(cutter.getCarNumber())
				.creationDate(creationDate)
				.from(cutted)
				.messageType(DriveMessageType.CUT_OFF)
				.message(DriveMessageType.CUT_OFF.getTitle())
				.smsMessage(DriveMessageType.CUT_OFF.getSms())
				.latitude(dialog.getLatitude())
				.longitude(dialog.getLongitude())
				.build();
		
		driveMessageRepository.save(toCutter);
		
		DriveMessage toCutted = DriveMessage.builder()
				.carNumberTo(cutted.getCarNumber())
				.creationDate(creationDate)
				.from(cutter)
				.messageType(DriveMessageType.CUT_OFF_SORRY)
				.message(DriveMessageType.CUT_OFF_SORRY.getTitle())
				.smsMessage(DriveMessageType.CUT_OFF_SORRY.getSms())
				.latitude(dialog.getLatitude())
				.longitude(dialog.getLongitude())
				.build();
		
		driveMessageRepository.save(toCutted);
		
	}

	private Person createFakePerson(DialogPerson personDto) {
		return Person.builder()
		.carName(personDto.getColor() + " " + personDto.getBrand())
		.carNumber(PlateValidator.processNumber(personDto.getNumber()))
		.registrationDate(new Date())
		.role(Role.FAKE)
		.build();
	}
		
}

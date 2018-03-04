package com.wyc.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wyc.controller.UIException;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.repository.DriveMessageDeliveryRepository;
import com.wyc.db.repository.DriveMessageRepository;

@Service
public class AnswerService {

	@Autowired
	private DriveMessageDeliveryRepository driveMessageDeliveryRepository;
	
	@Autowired
	private DriveMessageRepository driveMessageRepository;
	
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
}

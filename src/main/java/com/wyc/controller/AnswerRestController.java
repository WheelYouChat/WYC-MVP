package com.wyc.controller;

import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.dto.DriveMessageDto;
import com.wyc.service.AnswerService;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class AnswerRestController extends ExceptionHandlerController {
	
	@Autowired
	private AnswerService answerService;

	@Data
	@ToString
	public static class AnswerRequest {
		private String phone;
		private String code;
		private String answer;
	}
	@Data
	@ToString
	public static class AnswerResponse {
		
	}
	
	@RequestMapping(path="/api/messages/{code}", method=RequestMethod.GET)
	public DriveMessageDto getMessage(@PathVariable("code") String code) {
		DriveMessageDelivery delivery = answerService.getDriveMessageDelivery(code).orElseThrow(() -> new ResourceNotFoundException("Cannot find delivery by code '" + code + "'"));;
		DriveMessage msg = delivery.getDriveMessage();
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
		DriveMessageDto driveMessageDto = modelMapper.map(msg, DriveMessageDto.class);
		
		List<DriveMessage> answers = answerService.getAnswers(msg.getId());
		if(!answers.isEmpty()) {
			DriveMessageDto answer = modelMapper.map(answers.get(0), DriveMessageDto.class);
			driveMessageDto.setAnswer(answer);
		}
		
		return driveMessageDto;
	}
	
	
	@RequestMapping(path="/api/answer", method=RequestMethod.POST)
	public DriveMessage answer(@RequestBody AnswerRequest request) {
		if(request.getPhone().trim().length() != 4) {
			throw new UIException("Телефонный номер должен содержать 4 цифры", "phone");
		}
		
		DriveMessageDelivery delivery = answerService.getDriveMessageDelivery(request.getCode()).orElseThrow(() -> new ResourceNotFoundException("Cannot find delivery by code '" + request.getCode() + "'"));;
		String phoneNumber = delivery.getPhoneNumber();
		// Проверяем телефонный номер
		if(phoneNumber == null || !phoneNumber.endsWith(request.getPhone().trim())) {
			throw new UIException("Неправильный номер", "phone");
		}
		
		DriveMessage message = delivery.getDriveMessage();
		
		if(request.getAnswer() == null) {
			throw new UIException("Не выбран вариант ответа", "answer");
		}
		DriveMessageType messageType;
		try {
			messageType = DriveMessageType.valueOf(request.answer);
		} catch (IllegalArgumentException e) {
			throw new UIException("Неизвестный ответ", "answer");
		}
		DriveMessage answerMessage = answerService.answerTo(message, messageType);
		return answerMessage;
	}
	
	@Data
	public static class DialogPerson {
		private String number, brand, color;
	}
	
	@Data
	public static class Dialog {
		private DialogPerson cutter, cutted;
		private float longitude, latitude;
		private String locationTitle;
		private Date creationDate;
		
	}

	@RequestMapping(path="/makeDialogFaakkee", method=RequestMethod.POST)
	public Dialog makeFakeDialog(@RequestBody Dialog dialog) {
		answerService.makeFakeDialog(dialog);
		return dialog;
		
	}
	
	@RequestMapping(path="/dialogs", method=RequestMethod.GET)
	public List<Dialog> getFakeDialogs() {
		return answerService.getFakeDialogs();
	}
}

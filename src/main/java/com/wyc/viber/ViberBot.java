package com.wyc.viber;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyc.annotation.BotMethod;
import com.wyc.db.model.IncomingMessage;
import com.wyc.db.model.Person;
import com.wyc.db.repository.IncomingMessageRepository;
import com.wyc.db.repository.PersonRepository;
import com.wyc.service.AnswerService;
import com.wyc.service.MenuService;
import com.wyc.viber.ViberButton.ViberButtonActionType;
import com.wyc.viber.ViberButtonActionBody.Event;
import com.wyc.viber.ViberKeyBoard.ViberKeyBoardType;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ViberBot {

	@Autowired
	private ViberApi viberApi;
	
	@Autowired
	private MenuService menuService;
	
	@Autowired
	private PersonRepository personRepository;
	
	@Getter
	private boolean initialized = false;

	@Autowired
	private IncomingMessageRepository incomingMessageRepository;
	
	@Autowired
	private AnswerService answerService;
	
	
	public void initialize() throws IOException {
		if(!initialized) {
			viberApi.setWebHook();
			initialized = true;
		}
	}
	
	public void sendMenu(String to) throws IOException {
		ViberKeyBoard keyboard = viberApi.createMainMenu();
		viberApi.sendMessage(to, null, keyboard, null, "ЛихаЧат");
	}

	public void onMessageReceive(String body) {
		try {
			ViberIncomingMessage viberIncomingMessage = viberApi.parseIncommingMessage(body);
			if(viberIncomingMessage != null && viberIncomingMessage.getMessage_token() != null) {
				List<IncomingMessage> existingMessages = incomingMessageRepository.findByMessageId(viberIncomingMessage.getMessage_token());
				if(!existingMessages.isEmpty()) {
					// Такое сообщение уже приходило
					return;
				}
			}
		} catch(Exception e) { 
		}	
		
		IncomingMessage incomingMessage = IncomingMessage.builder()
				.body(body)
				.creationDate(new Date())
				.build();
		incomingMessageRepository.save(incomingMessage);
		
		try {
			ViberIncomingMessage viberIncomingMessage = viberApi.parseIncommingMessage(body);
			if(viberIncomingMessage.getSender() != null) {
				incomingMessage.setSenderId(viberIncomingMessage.getSender().getId());
			}
			if(viberIncomingMessage.getMessage() != null) {
				incomingMessage.setText(viberIncomingMessage.getMessage().getText());
			}
			incomingMessage.setMessageId(viberIncomingMessage.getMessage_token());
			
			if(viberIncomingMessage.getMessage() != null && viberIncomingMessage.getMessage().getLocation() != null) {
				incomingMessage.setLatitude(viberIncomingMessage.getMessage().getLocation().getLatitude());
				incomingMessage.setLongitude(viberIncomingMessage.getMessage().getLocation().getLongitude());
			}

			incomingMessageRepository.save(incomingMessage);

			if(viberIncomingMessage.getSender() != null && viberIncomingMessage.getSender().getId() != null) {
				checkUser(viberIncomingMessage.getSender());
			}
			
		} catch (IOException e) {
			log.error("Error parsing", e);
		}
		
		try {
			String data = null;
			if(incomingMessage.getText() != null) {
				ObjectMapper mapper = new ObjectMapper();
				ViberButtonActionBody actionBody = mapper.readValue(incomingMessage.getText(), ViberButtonActionBody.class);
				if(actionBody.getBeanName() != null) {
					data = actionBody.getBeanName() + "." + actionBody.getMethodName();
					if(actionBody.getParams() != null) {
						for(String p : actionBody.getParams()) {
							data = data + "." + p;
						}
					}
				} else {
					
				}
			}
			if(data != null) {
				incomingMessage.setData(data);
				incomingMessageRepository.save(incomingMessage);
			}
		} catch(Exception e) {
			log.error("Error", e);
		}
		answerService.answerToIncommingMessage(incomingMessage, viberApi);
	}

	protected void checkUser(ViberSender sender) {
		Optional<Person> optPerson = personRepository.findByViberId(sender.getId());
		if(!optPerson.isPresent()) {
			Person person = Person.builder()
					.viberId(sender.getId())
					.lastName(sender.getName())
					.avatar(sender.getAvatar())
					.build();
			personRepository.save(person);
		}
	}
}

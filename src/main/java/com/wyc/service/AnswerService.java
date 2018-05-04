package com.wyc.service;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyc.Location;
import com.wyc.MethodDesc;
import com.wyc.annotation.BotMethod;
import com.wyc.annotation.BotMethodParam;
import com.wyc.annotation.BotService;
import com.wyc.annotation.BotUser;
import com.wyc.annotation.ReplyMessageId;
import com.wyc.chat.BotParamValidator;
import com.wyc.chat.BotParamValidatorExt;
import com.wyc.chat.EnumMenu;
import com.wyc.chat.HasColor;
import com.wyc.chat.HasTitle;
import com.wyc.chat.validator.PlateValidator;
import com.wyc.controller.AnswerRestController.Dialog;
import com.wyc.controller.AnswerRestController.DialogPerson;
import com.wyc.controller.UIException;
import com.wyc.db.model.ContextItem;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.model.HasRoles;
import com.wyc.db.model.IncomingMessage;
import com.wyc.db.model.Person;
import com.wyc.db.model.Person.Role;
import com.wyc.db.model.PersonContext;
import com.wyc.db.repository.ContextItemRepository;
import com.wyc.db.repository.DriveMessageDeliveryRepository;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.PersonContextRepository;
import com.wyc.db.repository.PersonRepository;
import com.wyc.viber.ViberApi;
import com.wyc.viber.ViberButton;
import com.wyc.viber.ViberButton.ViberButtonActionType;
import com.wyc.viber.ViberButtonActionBody;
import com.wyc.viber.ViberKeyBoard;
import com.wyc.viber.ViberKeyBoard.ViberKeyBoardType;
import com.wyc.viber.ViberMessage;
import com.wyc.viber.ViberMessage.ViberMessageType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnswerService {

	@Autowired
	private DriveMessageDeliveryRepository driveMessageDeliveryRepository;
	
	@Autowired
	private DriveMessageRepository driveMessageRepository;
	
	@Autowired
	private PersonRepository personRepository;
	
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private PersonContextRepository personContextRepository;

	@Autowired
	private ContextItemRepository contextItemRepository;
	
	@Autowired
	private ViberApi viberApi;
	
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
	
	public List<Dialog> getFakeDialogsTurnRight() {
		List<Dialog> res = new ArrayList<>();
		
		List<DriveMessage> messages = new ArrayList(driveMessageRepository.findByMessageType(DriveMessageType.TURN_RIGHT_WRONG_LANE));
		Collections.sort(messages, new Comparator<DriveMessage>() {

			@Override
			public int compare(DriveMessage m1, DriveMessage m2) {
				return -m1.getCreationDate().compareTo(m2.getCreationDate());
			}
		});
		
		for(DriveMessage m : messages) {
			Dialog dialog = new Dialog();
			dialog.setLocationTitle(m.getLocationTitle());
			dialog.setLongitude(m.getLongitude());
			dialog.setLatitude(m.getLatitude());
			dialog.setCreationDate(m.getCreationDate());
			
			DialogPerson cutter =  new DialogPerson();
			cutter.setNumber(m.getCarNumberTo());
			dialog.setCutter(cutter);
			res.add(dialog );
		}

		return res;
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

	public void makeFakeDialogTurnRight(Dialog dialog) {
		Person p = personRepository.findOne(174L);
		if(p == null) {
			p = personRepository.findOne(1481L);
		}
		DriveMessage msg = DriveMessage.builder()
				.carNumberTo(dialog.getCutter().getNumber())
				.creationDate(new Date())
				.from(p)
				.messageType(DriveMessageType.TURN_RIGHT_WRONG_LANE)
				.message(DriveMessageType.TURN_RIGHT_WRONG_LANE.getTitle())
				.smsMessage(DriveMessageType.TURN_RIGHT_WRONG_LANE.getSms())
				.latitude(dialog.getLatitude())
				.longitude(dialog.getLongitude())
				.build();
		driveMessageRepository.save(msg);
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

	protected Optional<Person> getFrom(IncomingMessage incomingMessage) {
		Optional<Person> res = Optional.<Person>empty();
		try {
			int telegramId = Integer.parseInt(incomingMessage.getSenderId());
			res = personRepository.findByTelegramId(telegramId );
		} catch(NumberFormatException nfe) {
			// It's ok
		}
		if(!res.isPresent() && incomingMessage.getSenderId() != null) {
			// Не нашли по telegram Id = ищем по viberId
			log.info("Sender ID = " + incomingMessage.getSenderId());
			res = personRepository.findByViberId(incomingMessage.getSenderId());
		}
		return res;
	}
	
	/**
	 * Метод, который генерирует ответ на сообщение
	 * @param incomingMessage
	 */
	public void answerToIncommingMessage(IncomingMessage incomingMessage, ViberApi viberApi) {
		Optional<Person> userOpt = getFrom(incomingMessage); 
		userOpt.ifPresent((user) ->
		{
			String data = incomingMessage.getData();
			if(isEnum(data)) {
				String enumValue = data.substring(1);
				// TODO implement it
				// processAnswer(enumValue, null, null, from.getId().toString(), callback.getMessage().getMessageId(), callback.getInlineMessageId(), from);
			} else if(isMethod(data)) {
				String methodId = data;
				
				
				findMethod(methodId).ifPresent((bm) -> {
					// Remove previous context
					Optional<PersonContext> personContextOptional = personContextRepository.findByPersonId(user.getId());
					personContextOptional.ifPresent(pc -> {
						removeLastMessage(pc);
						List<ContextItem> items = contextItemRepository.findByPersonContextId(pc.getId());
						items.forEach(item -> {contextItemRepository.delete(item);});
						personContextRepository.delete(pc);
					});
					
					Method method = bm.getMethod();

					String text = incomingMessage.getText();
					ObjectMapper mapper = new ObjectMapper();
					ViberButtonActionBody actionBody = null;
					try {
						actionBody = mapper.readValue(text, ViberButtonActionBody.class);
					} catch (IOException e1) {
						log.error("Error parsing action body", e1);
					}

					// Create new context
					PersonContext personContext = PersonContext.builder()
							.id(user.getId() + " - " + methodId)
							.method(methodId)
							.person(user)
							.creationDate(new Date())
							.lastActivityDate(new Date())
							.build();
					personContextRepository.save(personContext);
					personContext = prepareContext(personContext, method, methodId, /* incomingMessage.getMessageId() */ actionBody.getReplyToMessageId());
					if(contextIsReady(personContext, method)) {
						invoke(bm.getBean(), personContext, method, null);
					} else {
						ViberMessage sendMessage = getNextQuestion(personContext, method, null);
	
						// sendMessage.setChatId(user.getTelegramId().toString());
	
						try {
							viberApi.sendMessage(user.getViberId(), sendMessage, "ЛихаЧат");
							// personContext.setLastMessageId(sentMessage.getMessageId());
							personContextRepository.save(personContext);
						} catch (IOException e) {
							log.error("Error sending message " + sendMessage, e);
						}
					}
					
				});
			} else if(incomingMessage.getText() != null || incomingMessage.getLongitude() != null) {
				// Ввели текст
				processAnswer(incomingMessage);
			}
		});
	}
	
	private PersonContext prepareContext(PersonContext personContext, Method method, String methodId, Long replyMessageId) {
		personContext.setItems(new ArrayList<>());

		String[] parts = parseMethodId(methodId);
		for(int i = 2; i < parts.length; i++) {
			String value = parts[i];
			ContextItem ci = ContextItem.builder()
					.id(personContext.getId() + " - " + (i - 2))
					.idx(i - 2)
					.value(value)
					.personContext(personContext)
					.creationDate(new Date())
					.build();
			contextItemRepository.save(ci);
		}
		
		Annotation[][] paramsAnnotations = method.getParameterAnnotations();
		
		for(int i = 0; i < paramsAnnotations.length; i++) {
			Annotation[] paramAnnotations = paramsAnnotations[i];
			for(Annotation paramAnnotation : paramAnnotations) {
				if(paramAnnotation instanceof BotUser) {
					ContextItem ci = ContextItem.builder()
							.id(personContext.getId() + " - " + i)
							.idx(i)
							.value(personContext.getPerson().getViberId())
							.personContext(personContext)
							.creationDate(new Date())
							.build();
					contextItemRepository.save(ci);
				} else if(paramAnnotation instanceof ReplyMessageId) {
					ContextItem ci = ContextItem.builder()
							.id(personContext.getId() + " - " + i)
							.idx(i)
							.value(replyMessageId.toString())
							.personContext(personContext)
							.creationDate(new Date())
							.build();
					contextItemRepository.save(ci);
				}
			}
		}

		return personContext;
	}

	public boolean contextIsReady(PersonContext personContext, Method method) {
		List<ContextItem> items = contextItemRepository.findByPersonContextId(personContext.getId());
		if(items != null && items.size() == method.getParameterCount()) {
			return true;
		}
		return false;
	}
	
		
	private void removeLastMessage(PersonContext pc) {
		// TODO Auto-generated method stub
		
	}

	public boolean isEnum(String data) {
		return data != null && data.startsWith("-");
	}
	
	public boolean isMethod(String data) {
		return data != null && parseMethodId(data).length >= 2;
	}

	public String[] parseMethodId(String methodId) {
		return methodId.split("[.]");
	}



	public Optional<MethodDesc> findMethod(String methodId) {
		Optional<MethodDesc> res = Optional.<MethodDesc>empty();
		String[] parts = parseMethodId(methodId);
		if(parts.length >= 2) {
			Map<String, Object> beans = applicationContext.getBeansWithAnnotation(BotService.class);
			Object bean = beans.get(parts[0]);
			Optional<Method> mOpt = Arrays.stream(bean.getClass().getMethods()).filter(m -> m.getName().equals(parts[1])).findFirst();
			if(mOpt.isPresent()) {
				// res = Optional.<Pair<Object, Method>>of(Pair.<Object, Method>of(bean, mOpt.get()));
				String args[] = new String[parts.length - 2];
				for(int i = 2; i< parts.length; i++) {
					args[i - 2] = parts[i];
				}
				res = Optional.<MethodDesc>of(MethodDesc.builder()
						.bean(bean)
						.method(mOpt.get())
						.args(args)
						.build());
				
			}
		}
		return res;
	}

	
	private Optional<PersonContext> getPersonContext(Long personId) {
		return personContextRepository.findByPersonId(personId);
	}
	
	private void processAnswer(IncomingMessage msg) {
		Optional<Person> person = getFrom(msg);
		processAnswer(msg.getText(), msg, person.get(), msg.getId().intValue(), null, person.get());
	}
	
	
	public void processAnswer(String value, IncomingMessage message, Person contact, Integer replyToMessageId, String inlineMessageId, Person from) {
		// Handle answer for a question
		Location location = message;
		Optional<PersonContext> pcOpt = getPersonContext(from.getId());
		pcOpt.ifPresent(pc -> {
			
			Optional<MethodDesc> methodOpt = findMethod(pc.getMethod());
			methodOpt.ifPresent(bm -> {
				Method method = bm.getMethod();
				int idx = getNextQuestionIdx(pc, method);
				
				if(idx >= 0) {
					// Check
					List<String> errors = checkValue(method, idx, value, message, pc.getPerson());
					if(!errors.isEmpty()) {
						// Value is incorrect 
						String errorMessage = createErrorMessage(errors);
						ViberMessage sendMessage = getNextQuestion(pc, method, errorMessage);
						
						// ViberMessage sendMessage = new ViberMessage();
						
						// sendMessage.setText(errorMessage);

						try {
							viberApi.sendMessage(pc.getPerson().getViberId(), sendMessage, "ЛихаЧат");
						} catch (IOException e) {
							log.error("Error sending message " + sendMessage, e);
						}
					} else {
						// Value is OK
						String v = value;
						if(location != null && location.getLatitude() != null) {
							v = location.getLongitude() + ":" + location.getLatitude();
						}
						ContextItem contextItem = ContextItem.builder()
								.id(pc.getId() + " - " + idx)
								.idx(idx)
								.personContext(pc)
								.value(v)
								.contactFirstName(contact == null ? null : contact.getFirstName())
								.contactLastName(contact == null ? null : contact.getLastName())
								.contactPhoneNumber(contact == null ? null : contact.getPhoneNumber())
								.contactUserId(contact == null ? null : contact.getViberId())
								.creationDate(new Date())
								.build();
						contextItemRepository.save(contextItem);
						pc.setLastActivityDate(new Date());
						personContextRepository.save(pc);
						PersonContext pcNew = personContextRepository.findOne(pc.getId());
						
						Class<?> paramType = method.getParameterTypes()[idx];
						Annotation[] paramAnnotations = method.getParameterAnnotations()[idx];
						Optional<Annotation> botMethodAnnotationOpt = Arrays.stream(paramAnnotations).filter(a -> a instanceof BotMethodParam).findFirst();
						if(paramType.isEnum() && replyToMessageId != null) {
							Object[] enumValues = paramType.getEnumConstants();
							String finalV = v;
							Arrays.stream(enumValues).filter(ev -> ev.toString().equals(finalV)).findFirst().ifPresent(enumValue -> {
								/*
								EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
								editMessageReplyMarkup.setChatId(chatId);
								// editMessageReplyMarkup.setInlineMessageId(inlineMessageId);
								editMessageReplyMarkup.setMessageId(replyToMessageId);

								InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup(); // createInlineKeyboardForEnums(enumValue);
								editMessageReplyMarkup.setReplyMarkup(replyMarkup );
								// Change Enum
								try {
									editMessageReplyMarkup(editMessageReplyMarkup);
								} catch (TelegramApiException e) {
									log.error("Error editing reply markup.", e);
								}
								//*/
								
								if(botMethodAnnotationOpt.isPresent()) {
									/*
									BotMethodParam botMethodAnnotation = (BotMethodParam) botMethodAnnotationOpt.get();
									EditMessageText editMessageText = new EditMessageText();
									editMessageText.setChatId(chatId);
									editMessageText.setMessageId(replyToMessageId);
									String title = enumValue.toString();
									if(enumValue instanceof HasTitle) {
										title = ((HasTitle)enumValue).getTitle();
									}
									editMessageText.setText(botMethodAnnotation.title() + "\n\nВы выбрали :" + title);
									try {
										editMessageText(editMessageText);
									} catch(TelegramApiException e) {
										log.error("Error editing message text.", e);
										
									}
									//*/
								}
								
							});
						}
						
						
						if(contextIsReady(pcNew, method)) {
							invoke(bm.getBean(), pcNew, method, replyToMessageId);
						} else {
							ViberMessage sendMessage = getNextQuestion(pcNew, method, null);
							// sendMessage.setChatId(chatId);
	
							try {
								// Message sentMessage = 
								viberApi.sendMessage(pcNew.getPerson().getViberId(), sendMessage, "ЛихаЧат");
								// pcNew.setLastMessageId(sentMessage.getMessageId());
								personContextRepository.save(pcNew);
							} catch (IOException e) {
								log.error("Error sending message " + sendMessage, e);
							}
							
						}
					}
				}
			});
		});
		if(!pcOpt.isPresent()) {
			// Нет контекста- посылаем меню
			// ... с предупреждением, что профиль не заполнен
			String text = "Используйте меню, чтобы начать диалог";
			if((contact.getRole() == null || contact.getRole() == Role.PEDESTRIAN) && contact.getNickname() == null) {
				text = "Заполните свой профиль водителя (меню Заполнить профиль) и вам будут доступны все функции.";
			}
			ViberKeyBoard mainMenu = viberApi.createMainMenu(contact.getRole());
			ViberMessage sendMessage = ViberMessage
					.builder()
					.text(text)
					.type(ViberMessageType.text)
					.keyboard(mainMenu)
					.build();
			try {
				viberApi.sendMessage(contact.getViberId(), sendMessage, "ЛихаЧат");
			} catch (IOException e) {
				log.error("Error sending", e);
			}
		}
	}


	private void invoke(Object bean, PersonContext personContext, Method method, Integer replayToMessageId) {
		Person person = personContext.getPerson();
		Object[] args = new Object[method.getParameterCount()];
		List<ContextItem> items = contextItemRepository.findByPersonContextId(personContext.getId());
		for(ContextItem item : items) {
			Class<?> paramType = method.getParameterTypes()[item.getIdx()];
			if(paramType == String.class) {
				args[item.getIdx()] = item.getValue();
			} else if(paramType == int.class || paramType == Integer.class) {
				args[item.getIdx()] = Integer.parseInt(item.getValue());
			} else if(paramType == long.class || paramType == Long.class) {
				args[item.getIdx()] = Long.parseLong(item.getValue());
				/*
			} else if(paramType == IncomingMessage.class) {
				Contact contact = new Contact() {

					@Override
					public String getPhoneNumber() {
						return item.getContactFirstName();
					}

					@Override
					public String getFirstName() {
						return item.getContactLastName();
					}

					@Override
					public String getLastName() {
						return item.getContactPhoneNumber();
					}

					@Override
					public Integer getUserID() {
						return item.getContactUserId();
					}
				};
				
				Message replyToMessage = new Message() {

					@Override
					public Integer getMessageId() {
						return replayToMessageId;
					}
					
				};
				Message msg = new Message() {
					@Override
					public String getText() {
						return item.getValue();
					}

					@Override
					public Contact getContact() {
						return contact;
					}

					@Override
					public Message getReplyToMessage() {
						return replyToMessage;
					}

				};
				
				args[item.getIdx()] = msg;
				//*/
			} else if(paramType == Location.class) {
				String[] parts = item.getValue().split(":");
				Location location = new Location() {

					@Override
					public Float getLongitude() {
						return Float.parseFloat(parts[0]);
					}

					@Override
					public Float getLatitude() {
						return Float.parseFloat(parts[1]);
					}	private boolean isEnum(String data) {
						return data.startsWith("-");
					}
					
					private boolean isMethod(String data) {
						return parseMethodId(data).length >= 2;
					}

					
				};
				args[item.getIdx()] = location;
			} else if(paramType.isEnum()) {
				String itemValue = item.getValue();
				if(itemValue != null && itemValue.startsWith("-")) {
					itemValue = itemValue.substring(1);
				}
				String finalItemValue = itemValue;
				Optional<?> evOpt = Arrays.stream(paramType.getEnumConstants()).filter(ev -> {return ev.toString().equals(finalItemValue);}).findFirst();
				if(evOpt.isPresent()) {
					args[item.getIdx()] = evOpt.get();
				} else {
					throw new IllegalArgumentException("Cannot not find enum '" + item.getValue() + "' for type " + paramType);
				}
			} else {
				throw new IllegalArgumentException("Cannot not call method " + method);
			}
		}
		try {
			Object result = method.invoke(bean, args);
			String successMessage = getSuccessMessage(method);
			if(result == null) {
				result = successMessage;
			}
			if(result instanceof String) {
				result = new String[]{result.toString()};
			}
			if(result instanceof String[]) {
				String [] arrResult = (String[]) result;
				for(String text : arrResult) {
					ViberKeyBoard mainMenu = viberApi.createMainMenu(person.getRole());
					ViberMessage sendMessage = ViberMessage
							.builder()
							.text(text)
							.type(ViberMessageType.text)
							.keyboard(mainMenu)
							.build();
					try {
						viberApi.sendMessage(person.getViberId(), sendMessage, "ЛихаЧат");
					} catch (IOException e) {
						log.error("Error sending", e);
					}
				}
			}
			
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("Error sending", e);
		}
		clearContext(personContext.getPerson().getId());
	}


	public void clearContext(Long personId) {
		Optional<PersonContext> pc = personContextRepository.findByPersonId(personId);
		pc.ifPresent(personContext -> {
			List<ContextItem> items = contextItemRepository.findByPersonContextId(personContext.getId());
			items.forEach(item -> contextItemRepository.delete(item));
			
			personContextRepository.delete(personContext);
		});
	}


	private String getSuccessMessage(Method m) {
		BotMethod a = m.getAnnotation(BotMethod.class);
		if(a != null) {
			return a.successMessage();
		}
		return "ok";
	}

	
	private ViberMessage getNextQuestion(PersonContext personContext, Method method, String errorMessage) {
		Annotation[][] paramsAnnotations = method.getParameterAnnotations();
		
		ContextItem[] items = new ContextItem[paramsAnnotations.length];
		List<ContextItem> contextItems = contextItemRepository.findByPersonContextId(personContext.getId());
		contextItems.forEach(item -> {
			items[item.getIdx()] = item;
		});
		
		for(int i = 0; i < paramsAnnotations.length; i++) {
			if(items[i] == null) {
				Class<?> paramType = method.getParameterTypes()[i];
				String title = "param " + i;
				Annotation[] paramAnnotations = paramsAnnotations[i];
				for(Annotation paramAnnotation : paramAnnotations) {
					if(paramAnnotation instanceof BotMethodParam) {
						BotMethodParam botMethodParam = (BotMethodParam) paramAnnotation;
						title = botMethodParam.title();
					}
				}
				ViberMessage sendMessage = new ViberMessage();
				sendMessage.setText((errorMessage == null ? "" : errorMessage + "\n") + title);
				if(paramType == Location.class) {
					/*
					List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
					List<InlineKeyboardButton> row = new ArrayList<>();
					InlineKeyboardButton button = new InlineKeyboardButton();
					row.add(button);
					keyboard.add(row);
					//*/
					//sendMessage.setType(ViberMessageType.location);
					
				} else if(paramType.isEnum()) {
					ViberKeyBoard keyboard = createInlineKeyboardForEnums(personContext.getPerson().getRole(), paramType.getEnumConstants());
					sendMessage.setKeyboard(keyboard);
				}
				ViberKeyBoard backToMainMenu = viberApi.createMenuBackToMainMenu();
				ViberKeyBoard keyboard;
				keyboard = joinKeyBoard(backToMainMenu, sendMessage.getKeyboard());
				sendMessage.setKeyboard(keyboard);
				
				return sendMessage;
			}
		}
		return null;
		
	}
	
	protected ViberKeyBoard joinKeyBoard(ViberKeyBoard src, ViberKeyBoard dest) {
		dest = dest == null ? ViberKeyBoard.builder().Type(ViberKeyBoardType.keyboard).build() : dest; 
		List<ViberButton> buttons = dest.getButtons() == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(dest.getButtons()));
		buttons.addAll(Arrays.asList(src.getButtons()));
		dest.setButtons(buttons.toArray(new ViberButton[buttons.size()]));
		return dest;
	}
	
	protected ViberKeyBoard createInlineKeyboardForEnums(Role role, Object... enumValues) {
		List<ViberButton> buttons = new ArrayList<>();
		for(Object enumValue : enumValues) {
			boolean add = true;
			if(enumValue instanceof EnumMenu) {
				EnumMenu enumMenu = (EnumMenu) enumValue;
				add = enumMenu.isRootMenu();
			}
			if(add && enumValue instanceof HasRoles) {
				HasRoles hasRoles = (HasRoles) enumValue;
				if(hasRoles.getRoles() != null && hasRoles.getRoles().length > 0) {
					add = Arrays.stream(hasRoles.getRoles()).filter(r -> role == r).findFirst().isPresent();
				}
			}
			if(add) {
				ViberButton button = ViberButton.builder().ActionType(ViberButtonActionType.reply).build();
				button.setActionBody("-" + enumValue.toString());
				if(enumValue instanceof HasTitle) {
					HasTitle hasTitle = (HasTitle) enumValue;
					button.setText(hasTitle.getTitle());
				} else {
					button.setText(enumValue.toString());
				}
				if(enumValue instanceof HasColor) {
					HasColor hasColor = (HasColor) enumValue;
					button.setBgColor(hasColor.getColor());
				}
				buttons.add(button);
			}
		}
		ViberKeyBoard replyMarkup = ViberKeyBoard.builder()
				.Type(ViberKeyBoardType.keyboard)
				.Buttons(buttons.toArray(new ViberButton[buttons.size()]))
			.build();
		
		return replyMarkup;
	}
	
	
	private int getNextQuestionIdx(PersonContext personContext, Method method) {
		Annotation[][] paramsAnnotations = method.getParameterAnnotations();
		
		ContextItem[] items = new ContextItem[paramsAnnotations.length];
		List<ContextItem> contextItems = contextItemRepository.findByPersonContextId(personContext.getId());
		contextItems.forEach(item -> {
			items[item.getIdx()] = item;
		});
		
		for(int i = 0; i < paramsAnnotations.length; i++) {
			if(items[i] == null) {
				Annotation[] paramAnnotations = paramsAnnotations[i];
				for(Annotation paramAnnotation : paramAnnotations) {
					if(paramAnnotation instanceof BotMethodParam) {
						return i;
					}
				}
			}
		}
		return -1;
		
	}

	private List<String> checkValue(Method method, int idx, String text, IncomingMessage message, Person person) {
		Location location = message;
		List<String> res = new ArrayList<>();
		Class<?>[] paramTypes = method.getParameterTypes();
		if(paramTypes != null && paramTypes.length > idx && idx >= 0) {
			// Check location
			Class<?> paramType = paramTypes[idx];
			if(paramType == Location.class) {
				if(location == null || location.getLatitude() == null) {
					res.add("Пришлите точку на карте (у вас в меню есть пункт \"Отправить местопложение\")"); 
				}
			}
			if(paramType.isEnum()) {
				Optional<?> eOpt = Arrays.asList(paramType.getEnumConstants()).stream().filter(o -> {
					Enum e = (Enum) o;
					return ("-" + e.name()).equals(text);
				}).findFirst();
				if(!eOpt.isPresent()) {
					res.add("Выберите вариант из списка внизу экрана."); 
				}
			}
		}
		
		Annotation[] paramAnnotations = method.getParameterAnnotations()[idx];
		Optional<Annotation> botMethodParamOpt = Arrays.stream(paramAnnotations).filter(a -> {return a instanceof BotMethodParam;}).findFirst();
		if(botMethodParamOpt.isPresent()) {
			BotMethodParam botMethodParam = (BotMethodParam) botMethodParamOpt.get();
			for(Class<? extends BotParamValidator> validatorClass : botMethodParam.validators()) {
				BotParamValidator validator;
				try {
					validator = validatorClass.newInstance();
					List<String> newErrs = null;
					if(validator.getValidationClass() == String.class ||
							validator.getValidationClass() == IncomingMessage.class ||
							validator.getValidationClass() == Object.class) {
						if(validator instanceof BotParamValidatorExt) {
							BotParamValidatorExt validatorExt = (BotParamValidatorExt) validator;
							newErrs = validatorExt.validate(text, person);
						} else {
							newErrs = validator.validate(text);
						}
					} else {
						throw new IllegalArgumentException("Unknown validation class " + validator.getValidationClass());
					}
					res.addAll(newErrs);
				} catch (InstantiationException | IllegalAccessException e) {
					log.error("Error creating validator " + validatorClass, e);
				}
			}
		}
		return res;
	}

	private String createErrorMessage(List<String> errors) {
		return String.join("\n ", errors.toArray(new String[errors.size()])) + "\nПопробуйте еще раз.";
	}

}

package com.wyc.telegram;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Contact;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.wyc.annotation.BotMethod;
import com.wyc.annotation.BotMethodParam;
import com.wyc.annotation.BotService;
import com.wyc.annotation.BotUser;
import com.wyc.annotation.ReplyBotMethod;
import com.wyc.annotation.ReplyMessageId;
import com.wyc.chat.BotParamValidator;
import com.wyc.chat.EnumMenu;
import com.wyc.chat.HasTitle;
import com.wyc.db.model.Car;
import com.wyc.db.model.ContextItem;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.model.DriveMessageDelivery.DeliveryType;
import com.wyc.db.model.IncomingMessage;
import com.wyc.db.model.Person;
import com.wyc.db.model.PersonContext;
import com.wyc.db.repository.CarRepository;
import com.wyc.db.repository.ContextItemRepository;
import com.wyc.db.repository.DriveMessageDeliveryRepository;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.IncomingMessageRepository;
import com.wyc.db.repository.PersonContextRepository;
import com.wyc.db.repository.PersonRepository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class WYCBot extends TelegramLongPollingBot {
	
	private String botUsername, botToken;
	
	private PersonRepository personRepository;

	private CarRepository carRepository;

	private PersonContextRepository personContextRepository;
	
	private ContextItemRepository contextItemRepository;
	
	private ApplicationContext applicationContext;

	private DriveMessageRepository driveMessageRepository;

	private DriveMessageDeliveryRepository driveMessageDeliveryRepository;

	private IncomingMessageRepository incomingMessageRepository;


	@Override
	public void onUpdateReceived(Update update) {
		saveMessage(update);
		
		if(update.getMessage() != null) {
			Message msg = update.getMessage();
			
			DeleteMessage deleteMessage = new DeleteMessage();
			deleteMessage.setChatId(msg.getChatId().toString());
			deleteMessage.setMessageId(msg.getMessageId());
			try {
				deleteMessage(deleteMessage);
			} catch (TelegramApiException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			if(msg.getText() != null && msg.getText().equals("/start")) {
				
				// Contact contact = msg.getContact();
				User contact = msg.getFrom();
				
				Optional<Person> existingPersonOpt = personRepository.findByTelegramId(contact.getId());

				SendMessage sendMessage = new SendMessage();
				
				if(!existingPersonOpt.isPresent()) {
				
					Person p = Person.builder()
							.carNumber("")
							.userName(contact.getUserName())
							.firstName(contact.getFirstName())
							.lastName(contact.getLastName())
							.telegramId(contact.getId())
							.languageCode(contact.getLanguageCode())
							.registrationDate(new Date())
							.build();
					log.info("Register person " + p);

					personRepository.save(p);
					sendMessage.setText("Вы зарегистрированы.");
				} else {
					sendMessage.setText("Вы уже зарегистрированы.");
				}
				
				sendMessage.setChatId(msg.getChatId());
				
				sendMessage.setReplyToMessageId(msg.getMessageId());
				
				InlineKeyboardMarkup inlineKeyboardMarkup = createMenu();
				sendMessage.setReplyMarkup(inlineKeyboardMarkup);
				clearContext(contact.getId());
				
				try {
					sendMessage(sendMessage);
				} catch(TelegramApiException e) {
					log.error("Error sending message " + sendMessage, e);
				}
				try {
					sendMessage = new SendMessage();
					sendMessage.setChatId(msg.getChatId());
					ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
					List<KeyboardRow> keyboard = new ArrayList<>();
					KeyboardRow row = new KeyboardRow();
					KeyboardButton button = new KeyboardButton("/menu");
					button.setRequestContact(true);
					row.add(button);
					keyboard.add(row );
					keyboardMarkup.setKeyboard(keyboard );
					sendMessage.setReplyMarkup(keyboardMarkup);
					sendMessage.setText("Menu");
					sendMessage(sendMessage);
				} catch(TelegramApiException e) {
					log.error("Error sending message " + sendMessage, e);
				}
			} else {
				processAnswer(msg);
			}
			
		}
		
		if(update.getCallbackQuery() != null) {
			CallbackQuery callback = update.getCallbackQuery();
			User from = callback.getFrom();
			Person user = getPerson(from);
			String data = callback.getData();
			if(isEnum(data)) {
				String enumValue = data.substring(1);
				processAnswer(enumValue, null, null, from.getId().toString(), callback.getMessage().getMessageId(), callback.getInlineMessageId(), from);
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
					
					// Create new context
					PersonContext personContext = PersonContext.builder()
							.id(user.getId() + " - " + methodId)
							.method(methodId)
							.person(user)
							.creationDate(new Date())
							.build();
					personContextRepository.save(personContext);
					personContext = prepareContext(personContext, method, methodId, callback.getMessage().getMessageId());
					if(contextIsReady(personContext, method)) {
						invoke(bm.getBean(), personContext, method, null);
					} else {
						SendMessage sendMessage = getNextQuestion(personContext, method);
	
						sendMessage.setChatId(user.getTelegramId().toString());
	
						try {
							Message sentMessage = sendMessage(sendMessage);
							personContext.setLastMessageId(sentMessage.getMessageId());
							personContextRepository.save(personContext);
						} catch (TelegramApiException e) {
							log.error("Error sending message " + sendMessage, e);
						}
					}
					
				});
			} else if(callback.getMessage() != null) {
				
			}
		}
		
	}
	
	protected void saveMessage(Update update) {
		Integer senderId = null;
		IncomingMessage message = new IncomingMessage();
		if(update.getMessage() != null && update.getMessage().getFrom() != null) {
			senderId = update.getMessage().getFrom().getId();
			message.setText(update.getMessage().getText());
			Location location = update.getMessage().getLocation();
			if(location != null) {
				message.setLatitude(location.getLatitude());
				message.setLongitude(location.getLongitude());
			}
			
			Contact contact = update.getMessage().getContact();
			if(contact != null) {
				message.setContactFirstName(contact.getFirstName());
				message.setContactLastName(contact.getLastName());
				message.setContactPhoneNumber(contact.getPhoneNumber());
				message.setContactUserId(contact.getUserID());
			}
		}
		if(senderId == null && update.getCallbackQuery() != null && update.getCallbackQuery().getFrom() != null) {
			CallbackQuery callback = update.getCallbackQuery();
			senderId = callback.getFrom().getId();
			message.setData(callback.getData());
		}
				 
		if(senderId != null) {
			message.setSenderId(senderId);
			message.setCreationDate(new Date());
			incomingMessageRepository.save(message);
		}
	}

	private boolean isEnum(String data) {
		return data.startsWith("-");
	}
	
	private boolean isMethod(String data) {
		return parseMthodId(data).length >= 2;
	}
	
	private InlineKeyboardMarkup createMenu() {
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(BotService.class);

		InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
		inlineKeyboardMarkup.setKeyboard(keyboard);

		log.debug("Start response. Scan services");
		for(String name : beans.keySet()) {
			Object bean = beans.get(name);
			log.debug("  bot service " + name + " = " + bean);
			Class<? extends Object> cls = bean.getClass();
			for(Method m : cls.getMethods()) {
				if(m.isAnnotationPresent(BotMethod.class)) {
					BotMethod a = m.getAnnotation(BotMethod.class);
					
					List<InlineKeyboardButton> row = new ArrayList<>();
					InlineKeyboardButton button = new InlineKeyboardButton(a.title());
					button.setCallbackData(name + "." + m.getName());
					
					if(a.url() != null && a.url().length() > 0) {
						button.setUrl(a.url());
					}
					
					row.add(button);
					keyboard.add(row);
				}
			}
		}
		return inlineKeyboardMarkup;
	}
	
	private void processAnswer(Message msg) {
		processAnswer(msg.getText(), msg, msg.getContact(), msg.getChatId().toString(), msg.getMessageId(), null, msg.getFrom());
	}
	
	private void processAnswer(String value, Message message, Contact contact, String chatId, Integer replyToMessageId, String inlineMessageId, User from) {
		// Handle answer for a question
		Location location = message == null ? null : message.getLocation();
		Optional<PersonContext> pcOpt = getPersonContext(from);
		pcOpt.ifPresent(pc -> {
			
			Optional<MethodDesc> methodOpt = findMethod(pc.getMethod());
			methodOpt.ifPresent(bm -> {
				Method method = bm.getMethod();
				int idx = getNextQuestionIdx(pc, method);
				
				if(idx >= 0) {
					// Check
					List<String> errors = checkValue(method, idx, value, message);
					if(!errors.isEmpty()) {
						// Value is incorrect 
						String errorMessage = createErrorMessage(errors);
						SendMessage sendMessage = new SendMessage();
						sendMessage.setChatId(chatId);
						sendMessage.setReplyToMessageId(replyToMessageId);
						
						sendMessage.setText(errorMessage);

						try {
							Message sentMessage = sendMessage(sendMessage);
						} catch (TelegramApiException e) {
							log.error("Error sending message " + sendMessage, e);
						}
					} else {
						// Value is OK
						String v = value;
						if(location != null) {
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
								.contactUserId(contact == null ? null : contact.getUserID())
								.creationDate(new Date())
								.build();
						contextItemRepository.save(contextItem);
						PersonContext pcNew = personContextRepository.findOne(pc.getId());
						
						Class<?> paramType = method.getParameterTypes()[idx];
						Annotation[] paramAnnotations = method.getParameterAnnotations()[idx];
						Optional<Annotation> botMethodAnnotationOpt = Arrays.stream(paramAnnotations).filter(a -> a instanceof BotMethodParam).findFirst();
						if(paramType.isEnum() && replyToMessageId != null) {
							Object[] enumValues = paramType.getEnumConstants();
							String finalV = v;
							Arrays.stream(enumValues).filter(ev -> ev.toString().equals(finalV)).findFirst().ifPresent(enumValue -> {
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
								
								if(botMethodAnnotationOpt.isPresent()) {
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
								}
								
							});
						}
						
						
						if(contextIsReady(pcNew, method)) {
							invoke(bm.getBean(), pcNew, method, replyToMessageId);
						} else {
							SendMessage sendMessage = getNextQuestion(pcNew, method);
							sendMessage.setChatId(chatId);
	
							try {
								Message sentMessage = sendMessage(sendMessage);
								pcNew.setLastMessageId(sentMessage.getMessageId());
								personContextRepository.save(pcNew);
							} catch (TelegramApiException e) {
								log.error("Error sending message " + sendMessage, e);
							}
							
						}
					}
				}
			});
		});
	}

	private String createErrorMessage(List<String> errors) {
		return String.join("\n ", errors.toArray(new String[errors.size()])) + "\nПопробуйте еще раз.";
	}

	private List<String> checkValue(Method method, int idx, String text, Message message) {
		Location location = message == null ? null : message.getLocation();
		List<String> res = new ArrayList<>();
		Class<?>[] paramTypes = method.getParameterTypes();
		if(paramTypes != null && paramTypes.length > idx && idx >= 0) {
			// Check location
			Class<?> paramType = paramTypes[idx];
			if(paramType == Location.class) {
				if(location == null) {
					res.add("Пришлите точку на карте (геопозицию)"); 
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
					if(validator.getValidationClass() == String.class) {
						newErrs = validator.validate(text);
					} else if(validator.getValidationClass() == Message.class) {
						newErrs = validator.validate(message);
					} else if(validator.getValidationClass() == Object.class) {
						newErrs = validator.validate(text);
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
	private void invoke(Object bean, PersonContext personContext, Method method, Integer replayToMessageId) {
		
		Object[] args = new Object[method.getParameterCount()];
		List<ContextItem> items = contextItemRepository.findByPersonContextId(personContext.getId());
		for(ContextItem item : items) {
			Class<?> paramType = method.getParameterTypes()[item.getIdx()];
			if(paramType == String.class) {
				args[item.getIdx()] = item.getValue();
			} else if(paramType == int.class || paramType == Integer.class) {
				args[item.getIdx()] = Integer.parseInt(item.getValue());
			} else if(paramType == Message.class) {
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
					}
					
				};
				args[item.getIdx()] = location;
			} else if(paramType.isEnum()) {
				Optional<?> evOpt = Arrays.stream(paramType.getEnumConstants()).filter(ev -> {return ev.toString().equals(item.getValue());}).findFirst();
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
					
					SendMessage sendMessage = new SendMessage();
					sendMessage.setChatId(personContext.getPerson().getTelegramId().toString());
					sendMessage.setReplyToMessageId(replayToMessageId);
					sendMessage.setText(text);
					try {
						sendMessage(sendMessage);
					} catch (TelegramApiException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clearContext(personContext.getPerson().getTelegramId());
	}

	private String getSuccessMessage(Method m) {
		BotMethod a = m.getAnnotation(BotMethod.class);
		if(a != null) {
			return a.successMessage();
		}
		return "ok";
	}

	private void clearContext(Integer telegramId) {
		Optional<PersonContext> pc = personContextRepository.findByPersonTelegramId(telegramId);
		pc.ifPresent(personContext -> {
			List<ContextItem> items = contextItemRepository.findByPersonContextId(personContext.getId());
			items.forEach(item -> contextItemRepository.delete(item));
			
			personContextRepository.delete(personContext);
		});
	}

	private void removeLastMessage(PersonContext personContext) {
		if(personContext != null && personContext.getLastMessageId() != null) {
			DeleteMessage deleteMessage = new DeleteMessage(personContext.getPerson().getId().toString(), personContext.getLastMessageId());
			try {
				deleteMessage(deleteMessage);
			} catch (TelegramApiException e) {
				log.error("Error deleting message", e);
			}
		}
	}
	
	private PersonContext prepareContext(PersonContext personContext, Method method, String methodId, Integer replyMessageId) {
		personContext.setItems(new ArrayList<>());

		String[] parts = parseMthodId(methodId);
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
							.value(personContext.getPerson().getTelegramId().toString())
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

	private boolean contextIsReady(PersonContext personContext, Method method) {
		List<ContextItem> items = contextItemRepository.findByPersonContextId(personContext.getId());
		if(items != null && items.size() == method.getParameterCount()) {
			return true;
		}
		return false;
	}
	
	private SendMessage getNextQuestion(PersonContext personContext, Method method) {
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
				SendMessage sendMessage = new SendMessage();
				sendMessage.setText(title);
				if(paramType == Location.class) {
					List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
					List<InlineKeyboardButton> row = new ArrayList<>();
					InlineKeyboardButton button = new InlineKeyboardButton();
					row.add(button);
					keyboard.add(row);
					
				} else if(paramType.isEnum()) {
					InlineKeyboardMarkup replyMarkup = createInlineKeyboardForEnums(paramType.getEnumConstants());
					sendMessage.setReplyMarkup(replyMarkup );
				}
				return sendMessage;
			}
		}
		return null;
		
	}
	
	protected InlineKeyboardMarkup createInlineKeyboardForEnums(Object... enumValues) {
		List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
		for(Object enumValue : enumValues) {
			boolean add = true;
			if(enumValue instanceof EnumMenu) {
				EnumMenu enumMenu = (EnumMenu) enumValue;
				add = enumMenu.isRootMenu();
			}
			if(add) {
				List<InlineKeyboardButton> row = new ArrayList<>();
				InlineKeyboardButton button = new InlineKeyboardButton();
				button.setCallbackData("-" + enumValue.toString());
				if(enumValue instanceof HasTitle) {
					HasTitle hasTitle = (HasTitle) enumValue;
					button.setText(hasTitle.getTitle());
				} else {
					button.setText(enumValue.toString());
				}
				row.add(button);
				keyboard.add(row);
			}
		}
		InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();
		replyMarkup.setKeyboard(keyboard);
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
	
	private Person getPerson(User from) {
		return personRepository.findByTelegramId(from.getId()).orElse(null);
	}
	
	private Optional<PersonContext> getPersonContext(User from) {
		return personContextRepository.findByPersonTelegramId(from.getId());
	}

	@Builder
	@AllArgsConstructor
	@Getter
	public static class MethodDesc {
		private String beanName;
		private Object bean;
		private Method method;
		private String args[];
	}

	private String[] parseMthodId(String methodId) {
		return methodId.split("[.]");
	}

	private Optional<MethodDesc> findMethod(String methodId) {
		Optional<MethodDesc> res = Optional.<MethodDesc>empty();
		String[] parts = parseMthodId(methodId);
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
	
	public void deliveryMessages() {
		Iterable<DriveMessage> messages = driveMessageRepository.findByDeliveredIsFalseOrderByIdDesc();
		for(DriveMessage message : messages) {
			if(message.getTo() != null && message.getTo().getTelegramId() != null && (message.getLongitude() == null || message.getLocationTitle() != null)) {
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
					// Сообщение послали напрямую в Telegram (ответили)
					persons.add(message.getTo());
				}
				for(Person person : persons) {
					SendMessage sendMessage = new SendMessage();
					sendMessage.setChatId(person.getId().toString());
					String text = createMessageText(message);
					sendMessage.setText(text);
					
					ReplyKeyboard replyMarkup = createReplyButtons(message.getMessageType());
					if(replyMarkup != null) {
						sendMessage.setReplyMarkup(replyMarkup);
					}

					DriveMessageDelivery messageDelivery = DriveMessageDelivery.builder()
							.deliveredDate(new Date())
							.deliveryType(DeliveryType.TELEGRAM)
							.to(person)
							.driveMessage(message)
							.build();
					try {
						Message sentMessage = sendMessage(sendMessage);
						messageDelivery.setSentMessageId(sentMessage.getMessageId());
					} catch (TelegramApiException e) {
						log.error("Error delivering message", e);
						messageDelivery.setDeliveryException(e.toString());
					}
					driveMessageDeliveryRepository.save(messageDelivery);
					message.setDelivered(true);
					driveMessageRepository.save(message);
				}
			}
		}
	}

	private ReplyKeyboard createReplyButtons(DriveMessageType messageType) {
		InlineKeyboardMarkup res = null;
		
		List<MethodDesc> replyMethods = getReplyMethods();
		String dataPrefix = "";
		if(replyMethods.size() > 0) {
			MethodDesc methodDesc = replyMethods.get(0);
			dataPrefix = methodDesc.getBeanName() + "." + methodDesc.getMethod().getName() + ".";
		}
		if(messageType != null && messageType.getAnswers().length > 0) {
			res = new InlineKeyboardMarkup();
			List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
			res.setKeyboard(keyboard);
			for(DriveMessageType answer : messageType.getAnswers()) {
				List<InlineKeyboardButton> row = new ArrayList<>();
				InlineKeyboardButton button = new InlineKeyboardButton();
				
				button.setCallbackData(dataPrefix + answer.getName());
				button.setText(answer.getTitle());
				row.add(button);
				keyboard.add(row );
			}
		}
		return res;
	}
	
	private List<MethodDesc> getReplyMethods() {
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
			res = res + "Вам пишет " + message.getRepliedTo().getCarNumberTo() + "\n" + message.getMessage();
		}
		res = res + (message.getLocationTitle() == null ? "" : "\nМесто: " + message.getLocationTitle()) + "\n";

		if(message.getRepliedTo() != null) {
			res = res + "\n\nв ответ на ваше сообщение\n" + message.getRepliedTo().getMessage() + 
					"\n от " + sdf.format(message.getRepliedTo().getCreationDate());
			
		}
		
		return res;
	}
	
}

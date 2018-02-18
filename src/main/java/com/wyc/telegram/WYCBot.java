package com.wyc.telegram;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.data.util.Pair;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.wyc.annotation.BotMethod;
import com.wyc.annotation.BotMethodParam;
import com.wyc.annotation.BotService;
import com.wyc.annotation.BotUser;
import com.wyc.chat.BotParamValidator;
import com.wyc.chat.HasTitle;
import com.wyc.db.model.ContextItem;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.Person;
import com.wyc.db.model.PersonContext;
import com.wyc.db.repository.ContextItemRepository;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.PersonContextRepository;
import com.wyc.db.repository.PersonRepository;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class WYCBot extends TelegramLongPollingBot {
	
	private String botUsername, botToken;
	
	private PersonRepository personRepository;

	private PersonContextRepository personContextRepository;
	
	private ContextItemRepository contextItemRepository;
	
	private ApplicationContext applicationContext;

	private DriveMessageRepository driveMessageRepository;


	@Override
	public void onUpdateReceived(Update update) {
		
		if(update.getMessage() != null) {
			Message msg = update.getMessage();
			if(msg.getText() != null && msg.getText().equals("/start")) {
				
				// Contact contact = msg.getContact();
				User contact = msg.getFrom();
				
				Person p = Person.builder()
						.carNumber("")
						.userName(contact.getUserName())
						.firstName(contact.getFirstName())
						.lastName(contact.getLastName())
						.id(contact.getId())
						.languageCode(contact.getLanguageCode())
						.build();
				log.info("Register person " + p);

				personRepository.save(p);
				
				SendMessage sendMessage = new SendMessage();
				sendMessage.setChatId(msg.getChatId());
				
				sendMessage.setReplyToMessageId(msg.getMessageId());
				sendMessage.setText("Вы зарегистрированы.");
				
				InlineKeyboardMarkup inlineKeyboardMarkup = createMenu();
				sendMessage.setReplyMarkup(inlineKeyboardMarkup);
				clearContext(p.getId());
				
				try {
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
				processAnswer(enumValue, null, from.getId().toString(), null, from);
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
					
					Method method = bm.getSecond();
					
					// Create new context
					PersonContext personContext = PersonContext.builder()
							.id(user.getId() + " - " + methodId)
							.method(methodId)
							.person(user)
							.build();
					personContextRepository.save(personContext);
					personContext = prepareContext(personContext, method);
					if(contextIsReady(personContext, method)) {
						invoke(bm.getFirst(), personContext, method, null);
					} else {
						SendMessage sendMessage = getNextQuestion(personContext, method);
	
						sendMessage.setChatId(user.getId().toString());
	
						try {
							Message sentMessage = sendMessage(sendMessage);
							personContext.setLastMessageId(sentMessage.getMessageId());
							personContextRepository.save(personContext);
						} catch (TelegramApiException e) {
							log.error("Error sending message " + sendMessage, e);
						}
					}
					
				});
			}
		}
		
	}
	
	private boolean isEnum(String data) {
		return data.startsWith("-");
	}
	
	private boolean isMethod(String data) {
		return data.split("[.]").length == 2;
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
		processAnswer(msg.getText(), msg.getLocation(), msg.getChatId().toString(), msg.getMessageId(), msg.getFrom());
	}
	
	private void processAnswer(String value, Location location, String chatId, Integer replyToMessageId, User from) {
		// Handle answer for a question
		Optional<PersonContext> pcOpt = getPersonContext(from);
		pcOpt.ifPresent(pc -> {
			
			Optional<Pair<Object, Method>> methodOpt = findMethod(pc.getMethod());
			methodOpt.ifPresent(bm -> {
				Method method = bm.getSecond();
				int idx = getNextQuestionIdx(pc, method);
				
				if(idx >= 0) {
					// Check
					List<String> errors = checkValue(method, idx, value, location);
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
						ContextItem contextItem = new ContextItem();
						contextItem.setId(pc.getId() + " - " + idx);
						contextItem.setIdx(idx);
						contextItem.setPersonContext(pc);
						contextItem.setValue(v);
						contextItemRepository.save(contextItem);
						PersonContext pcNew = personContextRepository.findOne(pc.getId());
						if(contextIsReady(pcNew, method)) {
							invoke(bm.getFirst(), pcNew, method, replyToMessageId);
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

	private List<String> checkValue(Method method, int idx, String text, Location location) {
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
					List<String> newErrs = validator.validate(text);
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
					sendMessage.setChatId(personContext.getPerson().getId().toString());
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
		clearContext(personContext.getPerson().getId());
	}

	private String getSuccessMessage(Method m) {
		BotMethod a = m.getAnnotation(BotMethod.class);
		if(a != null) {
			return a.successMessage();
		}
		return "ok";
	}

	private void clearContext(Integer personId) {
		Optional<PersonContext> pc = personContextRepository.findByPersonId(personId);
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
	
	private PersonContext prepareContext(PersonContext personContext, Method method) {
		// TODO Auto-generated method stub
		personContext.setItems(new ArrayList<>());

		Annotation[][] paramsAnnotations = method.getParameterAnnotations();
		
		for(int i = 0; i < paramsAnnotations.length; i++) {
			Annotation[] paramAnnotations = paramsAnnotations[i];
			for(Annotation paramAnnotation : paramAnnotations) {
				if(paramAnnotation instanceof BotUser) {
					ContextItem ci = ContextItem.builder()
							.id(personContext.getId() + " - " + i)
							.idx(i)
							.value(personContext.getPerson().getId().toString())
							.personContext(personContext)
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
					List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
					Object[] enumValues=paramType.getEnumConstants();
					for(Object enumValue : enumValues) {
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
					InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();
					replyMarkup.setKeyboard(keyboard);
					sendMessage.setReplyMarkup(replyMarkup );
				}
				return sendMessage;
			}
		}
		return null;
		
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
		return personRepository.findOne(from.getId());
	}
	
	private Optional<PersonContext> getPersonContext(User from) {
		return personContextRepository.findByPersonId(from.getId());
	}

	private Optional<Pair<Object, Method>> findMethod(String methodId) {
		Optional<Pair<Object, Method>> res = Optional.<Pair<Object, Method>>empty();
		String[] parts = methodId.split("[.]");
		if(parts.length == 2) {
			Map<String, Object> beans = applicationContext.getBeansWithAnnotation(BotService.class);
			Object bean = beans.get(parts[0]);
			Optional<Method> mOpt = Arrays.stream(bean.getClass().getMethods()).filter(m -> m.getName().equals(parts[1])).findFirst();
			if(mOpt.isPresent()) {
				res = Optional.<Pair<Object, Method>>of(Pair.<Object, Method>of(bean, mOpt.get()));
			}
		}
		return res;
	}
	
	public void deliveryMessages() {
		Iterable<DriveMessage> messages = driveMessageRepository.findByDeliveredDateIsNullOrderByIdDesc();
		for(DriveMessage message : messages) {
			if(message.getLongitude() == null || message.getLocationTitle() != null) {
				String carNumber = message.getCarNumberTo();
				carNumber = carNumber.toUpperCase();
				carNumber = carNumber.replaceAll("[ ]+", "");
				List<Person> persons = personRepository.findByCarNumber(carNumber);
				for(Person person : persons) {
					SendMessage sendMessage = new SendMessage();
					sendMessage.setChatId(person.getId().toString());
					String text = createMessageText(message);
					sendMessage.setText(text);
					try {
						sendMessage(sendMessage);
						
						message.setDeliveredDate(new Date());
					} catch (TelegramApiException e) {
						log.error("Error delivering message", e);
						message.setDeliveryException(e.toString());
					}
					driveMessageRepository.save(message);
				}
			}
		}
	}

	protected String createMessageText(DriveMessage message) {
		return "Вам поступило сообщение от " + message.getFrom().getUserName() + "\n" + message.getMessage() + 
				(message.getLocationTitle() == null ? "" : "\nМесто: " + message.getLocationTitle());
	}
	
}

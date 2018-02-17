package com.wyc.telegram;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.data.util.Pair;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.wyc.annotation.BotMethod;
import com.wyc.annotation.BotMethodParam;
import com.wyc.annotation.BotService;
import com.wyc.annotation.BotUser;
import com.wyc.db.model.ContextItem;
import com.wyc.db.model.Person;
import com.wyc.db.model.PersonContext;
import com.wyc.db.repository.ContextItemRepository;
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


	@Override
	public void onUpdateReceived(Update update) {
		
		if(update.getMessage() != null) {
			Message msg = update.getMessage();
			if(msg.getText().equals("/start")) {
				
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
			String methodId = callback.getData();
			
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
					// TODO call it
				} else {
					String title = getNextQuestion(personContext, method);

					SendMessage sendMessage = new SendMessage();
					sendMessage.setChatId(user.getId().toString());
					
					sendMessage.setText(title);

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
		// Handle answer for a question
		Optional<PersonContext> pcOpt = getPersonContext(msg.getFrom());
		pcOpt.ifPresent(pc -> {
			
			Optional<Pair<Object, Method>> methodOpt = findMethod(pc.getMethod());
			methodOpt.ifPresent(bm -> {
				Method method = bm.getSecond();
				int idx = getNextQuestionIdx(pc, method);
				if(idx >= 0) {
					ContextItem contextItem = new ContextItem();
					contextItem.setId(pc.getId() + " - " + idx);
					contextItem.setIdx(idx);
					contextItem.setPersonContext(pc);
					contextItem.setValue(msg.getText());
					contextItemRepository.save(contextItem);
					PersonContext pcNew = personContextRepository.findOne(pc.getId());
					if(contextIsReady(pcNew, method)) {
						invoke(bm.getFirst(), pcNew, method, msg);
					} else {
						String title = getNextQuestion(pcNew, method);

						SendMessage sendMessage = new SendMessage();
						sendMessage.setChatId(msg.getChatId());
						
						sendMessage.setText(title);

						try {
							Message sentMessage = sendMessage(sendMessage);
							pcNew.setLastMessageId(sentMessage.getMessageId());
							personContextRepository.save(pcNew);
						} catch (TelegramApiException e) {
							log.error("Error sending message " + sendMessage, e);
						}
						
					}
				}
			});
		});
	}

	private void invoke(Object bean, PersonContext personContext, Method method, Message msg) {
		
		Object[] args = new Object[method.getParameterCount()];
		List<ContextItem> items = contextItemRepository.findByPersonContextId(personContext.getId());
		for(ContextItem item : items) {
			args[item.getIdx()] = item.getValue();
		}
		try {
			method.invoke(bean, args);
			
			SendMessage sendMessage = new SendMessage();
			sendMessage.setChatId(personContext.getPerson().getId().toString());
			String sucessMessage = getSuccessMessage(method);
			sendMessage.setReplyToMessageId(msg.getMessageId());
			sendMessage.setText(sucessMessage);
			try {
				sendMessage(sendMessage);
			} catch (TelegramApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
	
	private String getNextQuestion(PersonContext personContext, Method method) {
		Annotation[][] paramsAnnotations = method.getParameterAnnotations();
		
		ContextItem[] items = new ContextItem[paramsAnnotations.length];
		List<ContextItem> contextItems = contextItemRepository.findByPersonContextId(personContext.getId());
		contextItems.forEach(item -> {
			items[item.getIdx()] = item;
		});
		
		for(int i = 0; i < paramsAnnotations.length; i++) {
			if(items[i] == null) {
				String title = "param " + i;
				Annotation[] paramAnnotations = paramsAnnotations[i];
				for(Annotation paramAnnotation : paramAnnotations) {
					if(paramAnnotation instanceof BotMethodParam) {
						BotMethodParam botMethodParam = (BotMethodParam) paramAnnotation;
						title = botMethodParam.title();
					}
				}
				return title;
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
	
	
}

package com.wyc.telegram;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import com.wyc.WYCConfig;
import com.wyc.db.repository.ContextItemRepository;
import com.wyc.db.repository.PersonContextRepository;
import com.wyc.db.repository.PersonRepository;

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

	@PostConstruct
	public void initBot() throws TelegramApiRequestException {
		log.info("Registering WYC Bot...");
		ApiContextInitializer.init();
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
		WYCBot wycBot = wycConfig.getBot();
		wycBot.setPersonRepository(personRepository);
		wycBot.setPersonContextRepository(personContextRepository);
		wycBot.setContextItemRepository(contextItemRepository);
		wycBot.setApplicationContext(applicationContext);
		telegramBotsApi.registerBot(wycBot);
		log.info("WYC Bot is registered");
	}
}

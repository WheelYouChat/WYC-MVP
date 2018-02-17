package com.wyc.chat;

import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.wyc.telegram.WYCBot;

public interface ChatContextState {
	String getId();
	
	void askMessage(WYCBot bot, Update update, ChatContext chatContext) throws TelegramApiException;
	
	void onAnswerMessage(WYCBot bot, Update update, ChatContext chatContext);
}

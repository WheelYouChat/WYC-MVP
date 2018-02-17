package com.wyc.chat;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.wyc.telegram.WYCBot;

import lombok.Data;

@Data
public abstract class AskStringState implements ChatContextState {
	
	private String id;
	
	private String question;
	
	private String nextState;

	@Override
	public void askMessage(WYCBot bot, Update update, ChatContext chatContext) throws TelegramApiException {
		SendMessage sendMessage = new SendMessage();
		sendMessage.setChatId(update.getMessage().getChatId());
		sendMessage.setText(question);
		bot.sendMessage(sendMessage);
	}

	@Override
	public void onAnswerMessage(WYCBot bot, Update update, ChatContext chatContext) {
		doAction(update);
		chatContext.changeContext(nextState);
	}
	
	protected abstract void doAction(Update update);

}

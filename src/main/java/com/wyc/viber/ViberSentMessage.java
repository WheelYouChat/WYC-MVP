package com.wyc.viber;

import lombok.Data;

/**
 * Ответ от Viber Rest API при отсылке сообщения
 * Пример : {"status":0,"status_message":"ok","message_token":5166944591754445121}
 * @author ukman
 *
 */
@Data
public class ViberSentMessage {
	private int status;
	private String status_message;
	private String message_token;
}

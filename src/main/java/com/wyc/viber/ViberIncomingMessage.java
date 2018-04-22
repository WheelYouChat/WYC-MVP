package com.wyc.viber;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViberIncomingMessage {
	public static enum Event {
		webhook,
		seen,
		message,
		delivered,
		
	}
// {"event":"webhook","timestamp":1523740735376,"message_token":5166052762708238769}
	private Event event;
	
	private Date timestamp;
	
	private String message_token;
	
	private int status;
	
	private String status_message;
	
	private ViberSender sender;
	
	private ViberMessage message;
	
	private Boolean silent;
	
	// {"event":"message","timestamp":1523741142921,"message_token":5166054472492032273,"sender":{"id":"sSPFZVqFK9BNhd4qFve6Rw==","name":"\u0421\u0435\u0440\u0433\u0435\u0439","avatar":"https://media-direct.cdn.viber.com/download_photo?dlid=7r7rWNbS58dsxqtHo-B_kjdEHphoFvfkcgcEuGK3RPaPlzgV7erzxWysTmcSQwATwaOSPerd4JehOwusR9e6URrB3s_abU1xSTNkh896u7L_cMVQbpitLWZPpRqSbC6Z3NKFZA&fltp=jpg&imsz=0000","language":"ru","country":"RU","api_version":5},"message":{"text":"\u0416\u044D","type":"text"},"silent":false}
}

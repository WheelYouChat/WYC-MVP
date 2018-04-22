package com.wyc.viber;

import lombok.Data;

@Data
public class ViberBotConfig {
	private String token;
	private String webHookUrl;
	private String viberBaseUrl;
}

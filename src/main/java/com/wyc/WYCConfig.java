package com.wyc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.wyc.avinfo.AVInfoConfig;
import com.wyc.telegram.WYCBot;
import com.wyc.viber.ViberBotConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@ConfigurationProperties("wyc")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WYCConfig {
	private WYCBot bot;
	private ViberBotConfig viber;
	private AVInfoConfig avinfo;
	
	private boolean viberDelivery;
	private boolean smsDelivery;
	private boolean webHook;
	
}

package com.wyc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.wyc.avinfo.AVInfoConfig;
import com.wyc.telegram.WYCBot;
import com.wyc.viber.ViberBotConfig;

import lombok.Data;

@Configuration
@ConfigurationProperties("wyc")
@Data
public class WYCConfig {
	private WYCBot bot;
	private ViberBotConfig viber;
	private AVInfoConfig avinfo;
}

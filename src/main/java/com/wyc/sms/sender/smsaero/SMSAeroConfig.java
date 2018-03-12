package com.wyc.sms.sender.smsaero;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties("sms.aero")
@Data
public class SMSAeroConfig {
	private String username;
	private String hash;
	private String host;
	private String sign;
	private String channel;
}

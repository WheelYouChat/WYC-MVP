package com.wyc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.wyc.telegram.WYCBot;

import lombok.Data;

@Configuration
@ConfigurationProperties("wyc")
@Data
public class WYCConfig {
	private WYCBot bot;
}

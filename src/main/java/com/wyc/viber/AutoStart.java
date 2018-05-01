package com.wyc.viber;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.wyc.WYCConfig;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AutoStart {

	@Autowired
	private WYCConfig wycConfig;

	@Autowired
	private ViberBot viberBot;
	
	@Scheduled(fixedDelay=100000, initialDelay=5000)
	public void init() {
		// Стартовать сразу нельзя- потому что сразу после операции set_webhook viber пойдет его проверять,
		// а порт на прослушку еще не открыт- поэтому надо подождать какое-то время
		try {
			if(!viberBot.isInitialized() && wycConfig.isWebHook()) {
				viberBot.initialize();
			}
		} catch (IOException e) {
			log.error("Error starting" + viberBot, e);
		}
	}
}

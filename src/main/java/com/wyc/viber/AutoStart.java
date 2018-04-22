package com.wyc.viber;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoStart {

	@Autowired
	private ViberBot viberBot;
	
	@Scheduled(fixedDelay=100000, initialDelay=5000)
	public void init() {
		try {
			if(!viberBot.isInitialized()) {
				viberBot.initialize();
				
				// TODO remove it
				viberBot.sendMenu("sSPFZVqFK9BNhd4qFve6Rw==");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

package com.wyc.viber;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@RequestMapping("/viber")
@RestController
@Slf4j
public class ViberWebHookController {

	@Autowired
	private ViberApi viberApi;
	
	@Autowired
	private ViberBot viberBot;

	@Autowired
	private Environment environment;

	@RequestMapping(path ="/webhook", method=RequestMethod.GET)
	public String viberWebHookGet(@RequestParam(name="monitor", required=false, defaultValue="false") boolean monitor) throws IOException {
		log.info("viberWebHookGet monitor=" + monitor);
		if(monitor) {
			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.writeValueAsString(environment.getActiveProfiles()); 
		}
		viberBot.onMessageReceive("{}");
		return "0";
	}
	
	@RequestMapping("/webhook")
	public Integer viberWebHook(@RequestBody String body) throws IOException {
		log.info(body);
		viberBot.onMessageReceive(body);
		return 0;
	}

	@RequestMapping("/webhooktest")
	// Этот метод используется для проксирования через NGINX webhook запросов для test бота
	public Integer viberWebHookTest(@RequestBody String body) throws IOException {
		return viberWebHook(body);
	}
}

package com.wyc.viber;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RequestMapping("/viber")
@RestController
@Slf4j
public class ViberWebHookController {

	@Autowired
	private ViberApi viberApi;
	
	@Autowired
	private ViberBot viberBot;
	
	@RequestMapping("/webhook")
	public Integer viberWebHook(@RequestBody String body) throws IOException {
		log.info(body);
		viberBot.onMessageReceive(body);
		return 0;
	}
}

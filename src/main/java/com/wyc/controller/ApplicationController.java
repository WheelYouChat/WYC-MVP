package com.wyc.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wyc.annotation.BotService;

@RestController
@RequestMapping("/api")
public class ApplicationController {

	@Autowired
	private ApplicationContext applicationContext;

	@RequestMapping("/test")
	public String test() {
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(BotService.class);
		return "hello " + beans;
	}
}

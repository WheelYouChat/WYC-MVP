package com.wyc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Configuration
public class ApplicationController {

	@Autowired
	private ApplicationContext applicationContext;

	@RequestMapping("/")
	public String index() {
		return "_index2.html";
	}
}

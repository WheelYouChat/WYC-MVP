package com.wyc.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.wyc.avinfo.AVInfoService;
import com.wyc.db.model.AVICar;
import com.wyc.service.PageVisitService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AnswerController {
	
	@Autowired
	private PageVisitService pageVisitService;
	
	@Autowired
	private AVInfoService avinfoService;
	
	@RequestMapping(path="/{code:[A-Za-z0-9]{2,7}}", method=RequestMethod.GET)
	public String answerPage(@PathVariable("code") String code, HttpServletRequest request) {
		// AVICar car = avinfoService.getCar("К064ММ177");
		// AVICar car = avinfoService.getCar("В881УЕ777");
		// log.info("Car = " + car);
		
		pageVisitService.logPageVisit("answer-page", request);
		return "_answer.html";
	}
	
	@RequestMapping(path="/dialog_ffaakkee", method=RequestMethod.GET)
	public String makeFakeDialog(HttpServletRequest request) {
		pageVisitService.logPageVisit("dialog-fake", request);
		return "_dialogFake.html";
	}
	/*
	@RequestMapping(path="/a/{code:[0-9]+}/answer", method=RequestMethod.POST)
	@ResponseBody
	public String answer(@PathVariable("code") String code) {
		return "answer.html";
	}
	@RequestMapping("/")
	public String answerPage2() {
		return "answer.html";
	}
	@RequestMapping("/a/center2")
	public String center() {
		return "center.html";
	}
	@RequestMapping("/aa")
	public String answerPage3() {
		return "answer.html";
	}
	@RequestMapping("/aa/fff")
	public String answerPage4() {
		return "answer_aa.html";
	}
*/
}

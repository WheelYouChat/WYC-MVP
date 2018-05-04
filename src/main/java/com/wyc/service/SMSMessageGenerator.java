package com.wyc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.wyc.db.model.DriveMessage.DriveMessageType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SMSMessageGenerator {
	
	public static final String SITE_URL = "l6t.ru";

	public String generateSMSMessage(DriveMessageType messageType, Date when, String location, String code) {
		location = location.replaceAll("\\bулица\\b", "ул");
		location = location.replaceAll("\\bпроспект\\b", "пр");
		location = location.replaceAll("\\bплощадь\\b", "пл");
		location = location.replaceAll("\\bшоссе\\b", "ш");
		location = location.replaceAll("\\bпереулок\\b", "пер");
		location = location.replaceAll("\\bбульвар\\b", "б-р");
		location = location.replaceAll("\\bнабережная\\b", "наб");
		location = location.replaceAll("\\bмост\\b", "м");
		location = location.replaceAll("\\bпроезд\\b", "пр-д");
		Calendar c = Calendar.getInstance();
		c.setTime(when);
		int h = c.get(Calendar.HOUR_OF_DAY);
		int m = c.get(Calendar.MINUTE);
		if(m > 30) {
			h++;
		}
		String res = getLessThan70( 
				messageType.getSms() + " в " + h +  "ч. адр-" + location + ". Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + " в " + h +  "ч. адр-" + location + ".Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + " в " + h +  "ч. адр-" + location + ". Отв. " + SITE_URL + "/" + code,
				messageType.getSms() + " в " + h +  "ч. адр-" + location + ".Отв. " + SITE_URL + "/" + code,
				messageType.getSms() + " в" + h +  "ч. адр-" + location + ".Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + " в" + h +  "ч. адр-" + location + ". Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + " в" + h +  "ч.адр-" + location + ". Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + " в" + h +  "ч.адр-" + location + ".Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + " в" + h +  "ч. адр-" + location + ". Отв. " + SITE_URL + "/" + code,
				messageType.getSms() + " в" + h +  "ч. адр-" + location + ".Отв. " + SITE_URL + "/" + code,
				messageType.getSms() + " в" + h +  "ч.адр-" + location + ".Отв. " + SITE_URL + "/" + code,
				messageType.getSms() + " в" + h +  "ч-" + location + ".Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + " в" + h +  "ч-" + location + ".Отв. " + SITE_URL + "/" + code,
				messageType.getSms() + " адр-" + location + ".Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + " адр-" + location + ".Отв. " + SITE_URL + "/" + code,
				messageType.getSms() + "-" + location + ".Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + "-" + location + ".Отв. " + SITE_URL + "/" + code,
				messageType.getSms() + ".Ответить " + SITE_URL + "/" + code,
				messageType.getSms() + ".Отв. " + SITE_URL + "/" + code,
				messageType.getSms() + " " + SITE_URL + "/" + code,
				messageType.getSms() + " " + SITE_URL
				);
		log.info("res = " + res + " - " + res.length());
		return res;
	}
	
	private String getLessThan70(String ... ss) {
		List<String> lst = new ArrayList(Arrays.asList(ss));
		Collections.sort(lst, (s1, s2) -> s2.length() - s1.length());
		
		for(String s : lst) {
			log.info(s + " - " + s.length());
		}
		
		for(String s : lst) {
			if(s.length() <= 130) {
				return s;
			}
		}
		return "";
	}
	
	public static void main(String args[]) {
		SMSMessageGenerator generator = new SMSMessageGenerator();
		generator.generateSMSMessage(DriveMessageType.CUT_OFF, new Date(), "Петропавловская улица", "As");
	}
}

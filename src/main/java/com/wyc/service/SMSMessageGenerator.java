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
				messageType.getSms() + " в " + h +  "ч. место-" + location + ". Ответить LihaChat.ru/" + code,
				messageType.getSms() + " в " + h +  "ч. место-" + location + ".Ответить LihaChat.ru/" + code,
				messageType.getSms() + " в " + h +  "ч. место-" + location + ". Отв. LihaChat.ru/" + code,
				messageType.getSms() + " в " + h +  "ч. место-" + location + ".Отв. LihaChat.ru/" + code,
				messageType.getSms() + " в" + h +  "ч. место-" + location + ".Ответить LihaChat.ru/" + code,
				messageType.getSms() + " в" + h +  "ч. место-" + location + ". Ответить LihaChat.ru/" + code,
				messageType.getSms() + " в" + h +  "ч.место-" + location + ". Ответить LihaChat.ru/" + code,
				messageType.getSms() + " в" + h +  "ч.место-" + location + ".Ответить LihaChat.ru/" + code,
				messageType.getSms() + " в" + h +  "ч. место-" + location + ". Отв. LihaChat.ru/" + code,
				messageType.getSms() + " в" + h +  "ч. место-" + location + ".Отв. LihaChat.ru/" + code,
				messageType.getSms() + " в" + h +  "ч.место-" + location + ".Отв. LihaChat.ru/" + code,
				messageType.getSms() + " в" + h +  "ч-" + location + ".Ответить LihaChat.ru/" + code,
				messageType.getSms() + " в" + h +  "ч-" + location + ".Отв. LihaChat.ru/" + code,
				messageType.getSms() + " место-" + location + ".Ответить LihaChat.ru/" + code,
				messageType.getSms() + " место-" + location + ".Отв. LihaChat.ru/" + code,
				messageType.getSms() + "-" + location + ".Ответить LihaChat.ru/" + code,
				messageType.getSms() + "-" + location + ".Отв. LihaChat.ru/" + code,
				messageType.getSms() + ".Ответить LihaChat.ru/" + code,
				messageType.getSms() + ".Отв. LihaChat.ru/" + code,
				messageType.getSms() + " LihaChat.ru/" + code,
				messageType.getSms() + " LihaChat.ru"
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
			if(s.length() <= 70) {
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

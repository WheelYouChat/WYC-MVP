package com.wyc.service;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.repository.DriveMessageDeliveryRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CodeGenerator {
	
	public final int MAX_ATTEMPT = 4;
	
	@Autowired
	private DriveMessageDeliveryRepository driveMessageDeliveryRepository;
	
	public String generateString(int length) {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String res = RandomStringUtils.random(length, 
				alphabet +
				alphabet.toLowerCase() +
				"0123456789" +
				""
				);
		return res;
	}
	
	public String generateUniqueString() {
		int length = 2;
		while(length < 10) {
			for(int i = 0; i < MAX_ATTEMPT; i++) {
				String code = generateString(length);
				Optional<DriveMessageDelivery> delivery = driveMessageDeliveryRepository.findByCode(code);
				if(!delivery.isPresent()) {
					// Не нашли - можно использовать
					return code;
				}
			}
			// Все попытки на этой длине исчерпаны- переходим к более длинному
			length++;
		}
		// Это никогда не должно произойти
		throw new IllegalStateException("Cannot find unique code");
	}
}

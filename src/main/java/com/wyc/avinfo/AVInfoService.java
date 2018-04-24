package com.wyc.avinfo;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyc.WYCConfig;
import com.wyc.db.model.AVICar;
import com.wyc.db.repository.AVICarRepository;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
@Slf4j 
public class AVInfoService {
	
	@Autowired
	private AVICarRepository aviCarRepository;
	
	@Autowired
	private WYCConfig config;
	
	protected AVICar getRequest(String number) {
		AVIRequest res = new AVIRequest();
		
		OkHttpClient client = new OkHttpClient();
		
		String url = config.getAvinfo().getUrl() + "/api.ashx?key=" + config.getAvinfo().getToken() + "&gosnomer=" + number;

		Request request = new Request.Builder()
		        .url(url)
		        .build();

	    try {
    		Response response = client.newCall(request).execute();
	    	if(response.isSuccessful()) {
	    		// Получили ответ
	    		String json = response.body().string();
	    		log.info("Response from AVInfo for number '" + number + "' " + json);
	    		AVICar aviCar = AVICar.builder()
	    				.creationDate(new Date())
	    				.json(json)
	    				.number(number)
	    				.build();
	    		aviCarRepository.save(aviCar);
	    		
	    		// Парсим, находим номер телефона и снова сохраняем
	    		AVIRequest aviRequest = parseAVIRequest(aviCar.getJson());
	    		aviCar.setPhoneNumber(aviRequest.getPhoneNumber());
	    		aviCarRepository.save(aviCar);
	    		return aviCar;

	    	} else {
	    		log.error("Error requesting gosnumber '" + number + "' Error : " + response + "\n" + response.body());
	    		throw new RuntimeException("Error requesting gosnumber '" + number + "' Error : " + response + "\n" + response.body());
	    	}
	    } catch (IOException e) {
    		throw new RuntimeException("Error requesting gosnumber '" + number, e);
		}
	}
	
	protected AVIRequest parseAVIRequest(String json) throws IOException {
		ObjectMapper mapper = createObjectMapper();
		AVIRequest aviRequest = mapper.readValue(json, AVIRequest.class);
		return aviRequest;
	}

	protected ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}


	public static void main(String[] args) {
		AVInfoService avInfoService = new AVInfoService();
		AVICar car = avInfoService.getCar("К064ММ177");

		// System.out.println("Hello World " + request);
	}
	
	public AVICar getCar(String number) {
		List<AVICar> cars = aviCarRepository.findByNumberOrderByCreationDate(number);
		if(cars.size() > 0) {
			AVICar car = cars.get(cars.size() - 1);
			
			// Если телефон пустой и есть JSON пытаемся добыть телефон в JSON и сохранить
			if(StringUtils.isBlank(car.getPhoneNumber()) && !StringUtils.isBlank(car.getJson())) {
				try {
					AVIRequest aviRequest = parseAVIRequest(car.getJson());
					String phone = aviRequest.getPhoneNumber();
					if(!StringUtils.isBlank(phone)) {
						car.setPhoneNumber(phone);
						aviCarRepository.save(car);
					}
				} catch (IOException e) {
					log.warn("Error parsing json for AVICar number = '" + number + "'", e);
				}
			}
			return car;
		} else {
			// AVICar is not found in DB
			AVICar car = getRequest(number);
			return car;
		}
	}
}

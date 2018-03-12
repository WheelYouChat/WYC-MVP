package com.wyc.avinfo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.wyc.db.model.AVICar;
import com.wyc.db.repository.AVICarRepository;
import com.wyc.exception.ResourceNotFoundException;

import okhttp3.OkHttpClient;

public class AVInfoService {
	
	@Autowired
	private AVICarRepository aviCarRepository;
	
	public static String AV_INFO_APU_URL = "";
	
	public AVRequest getRequest(String number) {
		AVRequest res = new AVRequest();
		
		OkHttpClient client = new OkHttpClient();
		/*
		Request request = new Request.Builder()
		        .url(url)
		        .build();

		    try (Response response = client.newCall(request).execute()) {
		      return response.body().string();
		    }
		    //*/		
		return res ;
	}
	

	public static void main(String[] args) {
		AVInfoService avInfoService = new AVInfoService();
		AVRequest request = avInfoService.getRequest("");

		System.out.println("Hello World " + request);
	}
	
	public AVICar getCar(String number) {
		List<AVICar> cars = aviCarRepository.findByNumberOrderByCreationDate(number);
		if(cars.size() > 0) {
			AVICar car = cars.get(cars.size() - 1);
			return car;
		} else {
			// AVICar is not found

			// TODO implement it
			throw new ResourceNotFoundException("Cannot find avinfo car by '" + number + "'");
		}
	}
}

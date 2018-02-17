package com.wyc.avinfo;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class AVInfoService {
	
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
}

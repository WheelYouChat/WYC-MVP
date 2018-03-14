package com.wyc.sms.sender.smsaero;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.glassfish.grizzly.utils.Charsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.sms.sender.SMSDeliveryStatusProvider;
import com.wyc.sms.sender.SMSSender;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
@Slf4j
public class SMSAeroSender implements SMSSender, SMSDeliveryStatusProvider {

	@Autowired
	private SMSAeroConfig smsAeroConfig;
	
	private static final Map<Integer, DeliveryStatus> STATUS_MAP = ImmutableMap.<Integer, SMSDeliveryStatusProvider.DeliveryStatus>builder()
			.put(1, DeliveryStatus.DELIVERED)
			.put(8, DeliveryStatus.MODERATION)
			.put(6, DeliveryStatus.DECLINED)
			.put(0, DeliveryStatus.IN_PROGRESS)
			.put(2, DeliveryStatus.ERROR)
			.build();

	public static final MediaType JSON = 
			MediaType.parse("application/json; charset=utf-8");

	@Override
	public String sendMessage(String phoneNumber, String message) throws IOException {
		
		OkHttpClient client = createClient();
		String url = smsAeroConfig.getHost() + "/v2/sms/send?" + 
		"number=" + encode(phoneNumber) + 
		"&text=" + encode(message) +
		"&sign=" + encode(smsAeroConfig.getSign()) + 
		"&channel=" + smsAeroConfig.getChannel();
		Request request = new Request.Builder()
		      .url(url)
		      .build();
		  Response response = client.newCall(request).execute();
		  String responseStr = response.body().string();
		  log.info(responseStr);
		  return responseStr;
	}
	
	protected String encode(String s) throws UnsupportedEncodingException {
		return URLEncoder.encode(s, Charsets.UTF8_CHARSET.name());		
	}
	
	protected OkHttpClient createClient() {
		OkHttpClient client = new OkHttpClient.Builder()
			    .addInterceptor(new BasicAuthInterceptor(smsAeroConfig.getUsername(), smsAeroConfig.getHash()))
			    .build();
		return client;
	}
	
	public class BasicAuthInterceptor implements Interceptor {

	    private String credentials;

	    public BasicAuthInterceptor(String user, String password) {
	        this.credentials = Credentials.basic(user, password);
	    }

	    @Override
	    public Response intercept(Chain chain) throws IOException {
	        Request request = chain.request();
	        Request authenticatedRequest = request.newBuilder()
	                    .header("Authorization", credentials).build();
	        return chain.proceed(authenticatedRequest);
	    }

	}

	@Data
	public static class AeroMessageJson {
		private AeroMessageData data;
	}
	@Data
	public static class AeroMessageData {
		private long id;
		private int status;
		private String extendStatus;
	}
	
	@Override
	public DeliveryStatus getStatus(DriveMessageDelivery delivery) {
		try {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		AeroMessageJson aeroMessageJson = mapper.readValue(delivery.getSmsSentResponse(), AeroMessageJson.class);
		long id = aeroMessageJson.getData().getId();
		OkHttpClient client = createClient();
		String url = smsAeroConfig.getHost() + "/v2/sms/status?id=" + id; 
		Request request = new Request.Builder()
		      .url(url)
		      .build();
		  Response response = client.newCall(request).execute();
		  String responseStr = response.body().string();
		  log.info(responseStr);
		  if(response.isSuccessful()) {
			  AeroMessageJson aeroMessageResponse = mapper.readValue(responseStr, AeroMessageJson.class);
			  log.info("" + aeroMessageResponse);
			  int status = aeroMessageResponse.getData().getStatus();
			  DeliveryStatus res = STATUS_MAP.get(status);
			  if(res != null) {
				  return res;
			  }
		  }
		}catch(Exception e) {
			log.error("", e);
		}
		  return DeliveryStatus.IN_PROGRESS;
	}	
}

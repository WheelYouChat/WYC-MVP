package com.wyc.sms.sender.smsaero;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.glassfish.grizzly.utils.Charsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wyc.sms.sender.SMSSender;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
@Slf4j
public class SMSAeroSender implements SMSSender {

	@Autowired
	private SMSAeroConfig smsAeroConfig;

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
}

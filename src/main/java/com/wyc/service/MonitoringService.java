package com.wyc.service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wyc.WYCConfig;
import com.wyc.controller.MonitoringController.MonitoringInfo;
import com.wyc.controller.MonitoringController.MonitoringInfo.State;
import com.wyc.monitoring.MonitoringConfig;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
@Slf4j
public class MonitoringService {
	
	@Autowired
	private WYCConfig wycConfig;
	
	public MonitoringInfo[] getMonitorInfos() {
		MonitoringInfo[] res = new MonitoringInfo[wycConfig.getMonitoringConfigs().length];
		MonitoringConfig[] configs = wycConfig.getMonitoringConfigs();
		List<MonitoringConfig> configList = Arrays.asList(configs);
		configList.stream().parallel().forEach(mc -> {
			MonitoringInfo info = getMonitorInfo(mc);
			int idx = configList.indexOf(mc);
			res[idx] = info;
		});
		return res;
	}

	protected MonitoringInfo getMonitorInfo(MonitoringConfig monitoringConfig) {
		String url = monitoringConfig.getUrl();
		MonitoringInfo res = null;

	    try {
			OkHttpClient client = new OkHttpClient();
			
			Request request = new Request.Builder()
			        .url(url)
			        .build();
    		Response response = client.newCall(request).execute();
    		if(response.isSuccessful()) {
    			String body = response.body().string();
    			if(monitoringConfig.getRegexps() == null) {
    				// Регэкспов нет- ничего не надо проверять
        	    	res = MonitoringInfo.builder()
        	    			.state(State.OK)
        	    			// .message(body)
        	    			.build();
    				
    			} else {
    				boolean isOk = true;
    				for(String re : monitoringConfig.getRegexps()) {
    					Pattern p = Pattern.compile(re);
    					Matcher m = p.matcher(body);
    					if(!m.find()) {
    						// Не нашли
    						isOk = false;
                	    	res = MonitoringInfo.builder()
                	    			.state(State.ERROR)
                	    			.message("URL = " + url + "\nCannot find re = '" + re + "' in \n" + body)
                	    			.build();
                	    	break;
    					}
    				}
    				if(isOk) {
            	    	res = MonitoringInfo.builder()
            	    			.state(State.OK)
            	    			.message("")
            	    			.build();
    				}
    			}
    		} else {
    	    	res = MonitoringInfo.builder()
    	    			.state(State.ERROR)
    	    			.message(response.body().string())
    	    			.build();
    			
    		}
	    } catch(Exception e) {
	    	log.error("Error monitoring url = '" + url + "'", e);
	    	res = MonitoringInfo.builder()
	    			.state(State.ERROR)
	    			.message(e.getMessage())
	    			.build();
	    }

		
		return res;
	}

}

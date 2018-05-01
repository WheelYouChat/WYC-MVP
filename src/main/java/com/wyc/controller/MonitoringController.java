package com.wyc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wyc.WYCConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/mntrng258")
public class MonitoringController {

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	private Environment environment;
	
	@Autowired 
	private WYCConfig wycConfig;
	
	@RequestMapping("/cnfg")
	public WYCConfig getConfig() {
		WYCConfig res = WYCConfig.builder()
				.smsDelivery(wycConfig.isSmsDelivery())
				.viberDelivery(wycConfig.isViberDelivery())
				.webHook(wycConfig.isWebHook())
				.build();
		return res;
	}
	
	@RequestMapping("/ping")
	public Integer ping() {
		return 0;
	}
	
	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ApplicationInfo {
		private String appName;
		private String[] profiles;
	}
	@RequestMapping("/info")
	public ApplicationInfo info() {
		return ApplicationInfo.builder()
				.appName(this.appName)
				.profiles(environment.getActiveProfiles())
				.build();
	}
	
}

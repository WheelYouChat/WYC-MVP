package com.wyc.avinfo;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AVIRequest {
	private boolean error;
	
	private AVIResult result;
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AVIResult {
		private AVIGibdd gibdd[];
		
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AVIGibdd {
		private String car;
		private String phone;
	}
	
	public String getPhoneNumber() {
		if(result != null && result.getGibdd() != null) {
			for(AVIGibdd gibdd : result.getGibdd()) {
				if(!StringUtils.isBlank(gibdd.getPhone())) {
					return gibdd.getPhone();
				}
			}
		}
		return null;
	}
}

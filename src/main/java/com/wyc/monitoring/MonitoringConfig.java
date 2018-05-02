package com.wyc.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonitoringConfig {
	private String title;
	private String url;
	private String[] regexps;
}

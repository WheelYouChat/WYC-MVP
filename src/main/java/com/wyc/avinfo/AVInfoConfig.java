package com.wyc.avinfo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AVInfoConfig {
	private String url;
	private String token;
	private List<String> areas;
}

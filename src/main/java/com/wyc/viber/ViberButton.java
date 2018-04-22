package com.wyc.viber;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class  ViberButton {
	public static enum ViberButtonActionType {
		reply, 
		
		@JsonProperty("open-url")
		open_url,

		@JsonProperty("location-picker")
		location_picker,

		@JsonProperty("share-phone")
		share_phone,
		none
	}
	
	private Integer Columns;
	private Integer Rows;
	private String BgColor;
	private Boolean Silent;
	
	private ViberButtonActionType ActionType; 
	private String ActionBody;
	private String Text;
}

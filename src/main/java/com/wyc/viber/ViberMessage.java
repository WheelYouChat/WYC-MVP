package com.wyc.viber;

import com.wyc.Location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViberMessage {
	
	public static enum ViberMessageType {
		text,
		picture,
		video,
		file,
		location,
		contact,
		sticker,
		carousel_content,
		url		
	}
	
	private String receiver;
	private String text;
	private ViberMessageType type;
	private ViberSender sender;
	private ViberKeyBoard keyboard;
	private String tracking_data;
	private String min_api_version;
	private ViberButton[] Buttons;
	private ViberLocation location;
	
}

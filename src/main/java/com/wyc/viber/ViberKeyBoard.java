package com.wyc.viber;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViberKeyBoard {
	
	public static enum ViberKeyBoardType {
		keyboard
	}

	private ViberButton[] Buttons;
	
	private String BgColor;
	
	private Boolean DefaultHeight;
	
	private ViberKeyBoardType Type;
}

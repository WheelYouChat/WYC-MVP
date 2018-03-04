package com.wyc.dto;

import lombok.Data;

@Data
public class DriveMessageTypeDto {
	private String name;
	private String title;
	private DriveMessageTypeDto answers[];
}

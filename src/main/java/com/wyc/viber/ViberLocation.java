package com.wyc.viber;

import com.wyc.Location;

import lombok.Data;

@Data
public class ViberLocation implements Location{
	private Float lon, lat;

	@Override
	public Float getLongitude() {
		return getLon();
	}

	@Override
	public Float getLatitude() {
		return getLat();
	}
}

package com.wyc.sms.sender;

import java.io.IOException;

public interface SMSSender {
	String sendMessage(String phoneNumber, String message) throws IOException;
}

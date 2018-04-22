package com.wyc;

import com.wyc.db.model.Person;

public interface Sender {
	void sendMessage(Person to, String message);
}

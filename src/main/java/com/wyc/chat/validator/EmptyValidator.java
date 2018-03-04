package com.wyc.chat.validator;

import java.util.Collections;
import java.util.List;

public class EmptyValidator extends BaseValidator {

	public EmptyValidator() {
		super(Object.class);
	}

	@Override
	public List<Object> validate(Object s) {
		return Collections.emptyList();
	}

}

package com.wyc.test.validator;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.wyc.chat.validator.PlateValidator;

import junit.framework.TestCase;

public class TestPlateValidator extends TestCase{

	@Test
	public void test() {
		PlateValidator plateValidator = new PlateValidator();

		// Correct
		List<String> error = plateValidator.validate("Р123СМ90");
		assertTrue(error.isEmpty());

		error = plateValidator.validate("Р123СМ999");
		assertTrue(error.isEmpty());
		
		error = plateValidator.validate("  Р 1 2 3 С М 9 9   9  ");
		assertTrue(error.isEmpty());
		
		error = plateValidator.validate("   1 2 3 Р    С М 9 9   9  ");
		assertTrue(error.isEmpty());
		
		// Incorrect
		error = plateValidator.validate("1111");
		assertFalse(error.isEmpty());

		error = plateValidator.validate("1111РР");
		assertFalse(error.isEmpty());
		
		error = plateValidator.validate("Р93СМ9");
		assertFalse(error.isEmpty());
		
		error = plateValidator.validate("Р93СМ94");
		assertFalse(error.isEmpty());
		
		error = plateValidator.validate("Р933СМ9433");
		assertFalse(error.isEmpty());
	}
}

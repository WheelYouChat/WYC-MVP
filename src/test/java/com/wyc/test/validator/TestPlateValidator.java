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
	
	public static String[] PROCESS_EXAMPLES = new String[]{
		"А000АА99", "А000АА99",
		"а000аа99", "А000АА99",
		"а000аа999", "А000АА999",
		"000аАа999", "А000АА999",
		"ААА000999", "А000АА999",
		"сми000999", "С000МИ999",
		"  с  м  и  0    0   0  7  7   ", "С000МИ77",
	};
	
	@Test
	public void testProcessing() {
		for(int i = 0; i < PROCESS_EXAMPLES.length; i+= 2) {
			String number = PROCESS_EXAMPLES[i];
			String actual = PROCESS_EXAMPLES[i + 1];
			assertEquals(PlateValidator.processNumber(number), actual);
		}
	}
}

package com.wyc.chat.validator;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jersey.repackaged.com.google.common.collect.Lists;

public class PlateValidator  extends BaseValidator<String>{

	public PlateValidator() {
		super(String.class);
	}

	private static Pattern PATTERNS[] = new Pattern[]{
			Pattern.compile("[а-яА-Я][0-9][0-9][0-9][а-яА-Я][а-яА-Я][0-9][0-9][0-9]?"),
			Pattern.compile("[0-9][0-9][0-9][а-яА-Я][а-яА-Я][а-яА-Я][0-9][0-9][0-9]?")
	};
	
	@Override
	public List<String> validate(String s) {
		// Remove spaces
		s = s.replaceAll("[ ]+", "");
		for(Pattern p : PATTERNS) {
			Matcher m = p.matcher(s);
			if(m.matches()) {
				return Collections.EMPTY_LIST;
			}
		}
		return Lists.newArrayList("Номер должен быть в формате 'А000АА001' или '000ААА001' с любым количеством пробелов.");
	}
	
	
	// А123АА678
	private static final int[] DIGIT_POSITIONS = new int[]{1, 2, 3, 6, 7, 8};
	private static final int[] LETTER_POSITIONS = new int[]{0, 4, 5};
	
	public static String processNumber(String newNumber) {
		String res = newNumber.replaceAll("[ ]+", "").toUpperCase();
		StringBuilder sb = new StringBuilder(res);
		int letterIdx = 0;
		int digitIdx = 0;
		for(int i = 0; i < res.length(); i++) {
			char c = res.charAt(i);
			int idx = -1;
			if(Character.isLetter(c)) {
				idx = LETTER_POSITIONS[letterIdx];
				letterIdx++;
			} else if(Character.isDigit(c)) {
				idx = DIGIT_POSITIONS[digitIdx];
				digitIdx++;
			}
			sb.setCharAt(idx, c);
		}
		return sb.toString();
	}

}

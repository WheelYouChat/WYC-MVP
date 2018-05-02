package com.wyc.chat.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jersey.repackaged.com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PlateValidator  extends BaseValidator<String>{
	
	// Допускать пустые номер (для пешеходов)
	private boolean allowEmpty = false;

	public PlateValidator() {
		this(false);
	}
	
	public PlateValidator(boolean allowEmpty) {
		super(String.class);
		this.allowEmpty = allowEmpty;
	}

	private static final String CANONICAL_PLATE_REGEXP = "([а-яА-Я])([0-9][0-9][0-9])([а-яА-Я][а-яА-Я])([0-9][0-9][0-9]?)";
	private static final Pattern CANONICAL_PLATE_PATTERN = Pattern.compile(CANONICAL_PLATE_REGEXP);
	private static Pattern PATTERNS[] = new Pattern[]{
			CANONICAL_PLATE_PATTERN,
			Pattern.compile("[0-9][0-9][0-9][а-яА-Я][а-яА-Я][а-яА-Я][0-9][0-9][0-9]?")
	};
	
	@Override
	public List<String> validate(String s) {
		if(allowEmpty && s.trim().length() == 0) {
			return new ArrayList<>();
		}
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

	@Getter
	@AllArgsConstructor
	@Builder
	public static class Plate {
		private final String letter1;
		private final String letter23;
		private final String digits;
		private final String area;
	}
	
	public static Plate parseNumber(String number) {
		String s = processNumber(number);
		Matcher m = CANONICAL_PLATE_PATTERN.matcher(s);
		if(m.find()) {
			Plate res = Plate.builder()
				.letter1(m.group(1))
				.digits(m.group(2))
				.letter23(m.group(3))
				.area(m.group(4))
				.build();
			return res;
		} else {
			throw new IllegalArgumentException("Can not parse number '" + number + "'");
		}
	}
}

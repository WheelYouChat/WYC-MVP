package com.wyc;

import java.lang.reflect.Method;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class MethodDesc {
	private String beanName;
	private Object bean;
	private Method method;
	private String args[];
}

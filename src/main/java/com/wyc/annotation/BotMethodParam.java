package com.wyc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.wyc.chat.BotParamValidator;
import com.wyc.chat.validator.EmptyValidator;

//Make the annotation available at runtime:
@Retention(RetentionPolicy.RUNTIME)
//Allow to use only on types:
@Target(ElementType.PARAMETER)
public @interface BotMethodParam {

	String title();
	
	Class<? extends BotParamValidator>[] validators() default EmptyValidator.class;
	
}

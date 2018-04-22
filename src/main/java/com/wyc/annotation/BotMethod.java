package com.wyc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotMethod {
	String title();
	String url() default "";
	String successMessage() default "ok";
	int order() default 0;
	int cols() default 3;
	int rows() default 1;
	
	/**
	 * Нужно ли показывать в главном меню
	 * @return
	 */
	boolean mainMenu() default false;
	
	/**
	 * Метод возврата в главное меню
	 * @return
	 */
	boolean backToMainMenu() default false;
}

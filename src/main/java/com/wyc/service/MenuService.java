package com.wyc.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.wyc.annotation.BotMethod;
import com.wyc.annotation.BotService;
import com.wyc.db.model.Person.Role;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MenuService {

	@Autowired
	private ApplicationContext applicationContext;

	public List<Pair<String, Method>> getMenuMethods(Role role) {
		List<Pair<String, Method>> botMethods = getBotMethods(BotMethod::mainMenu, role); 
		return botMethods;
	}
	
	public List<Pair<String, Method>> getBackToMenuMethods() {
		List<Pair<String, Method>> botMethods = getBotMethods(BotMethod::backToMainMenu, Role.PEDESTRIAN); 
		return botMethods;
	}


	protected List<Pair<String, Method>> getBotMethods(Predicate<BotMethod> filter, Role role) {
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(BotService.class);
		List<Pair<String, Method>> botMethods = new ArrayList<>(); 

		log.debug("Start response. Scan services");
		for(String name : beans.keySet()) {
			Object bean = beans.get(name);
			log.debug("  bot service " + name + " = " + bean);
			Class<? extends Object> cls = bean.getClass();
			Method[] methods = cls.getMethods();
			
			for(Method m : methods) {
				if(m.isAnnotationPresent(BotMethod.class)) {
					BotMethod botMethodAnnotation = m.getAnnotation(BotMethod.class);
					if(hasPermission(botMethodAnnotation, role) && filter.test(botMethodAnnotation)) {
						botMethods.add(Pair.of(name, m));
					}
				}
			}
			Collections.sort(botMethods, new Comparator<Pair<String, Method>>(){
				@Override
				public int compare(Pair<String, Method> p1, Pair<String, Method> p2) {
					Method m1 = p1.getSecond();
					Method m2 = p2.getSecond();
					BotMethod a1 = m1.getAnnotation(BotMethod.class);
					BotMethod a2 = m2.getAnnotation(BotMethod.class);
					return a1.order() - a2.order();
				}
			});
			
		}
		return botMethods;
		
	}
	
	/**
	 * Имеет ли пользователь право на данный метод?
	 * @param botMethod
	 * @param user
	 * @return
	 */
	protected boolean hasPermission(BotMethod botMethod, Role role) {
		if(botMethod.roles() == null || botMethod.roles().length == 0) {
			// Метод доступен всем
			return true;
		}
		for(Role r : botMethod.roles()) {
			if(r == role) {
				return true;
			}
		}
		return false;
	}
}

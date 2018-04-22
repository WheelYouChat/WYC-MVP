package com.wyc.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wyc.db.model.Person;
import com.wyc.db.model.PersonContext;
import com.wyc.db.repository.PersonContextRepository;

@Service
public class PersonService {

	@Autowired
	private PersonContextRepository personContextRepository;
	
	/**
	 * Определяет есть ли активный контекст у пользователя, если есть- значит пользователь занят
	 * @param p
	 * @return
	 */
	public boolean hasActiveContext(Person p) {
		Optional<PersonContext> personContextOpt = personContextRepository.findByPersonId(p.getId());
		return personContextOpt.isPresent();
	}
}

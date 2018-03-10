package com.wyc.db.repository;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.PersonContext;

@Repository
public interface PersonContextRepository extends PagingAndSortingRepository<PersonContext, String>{
	Optional<PersonContext> findByPersonId(Long personId);
	Optional<PersonContext> findByPersonTelegramId(Integer telegramId);
	void removeByPersonId(Long personId);
}

package com.wyc.db.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.Person;

@Repository
public interface PersonRepository extends PagingAndSortingRepository<Person, Long>{
	List<Person> findByCarNumber(String carNumber);
	List<Person> findByCarNumberOrIdIn(String carNumber, Collection<Integer> ids);
	Optional<Person> findByTelegramId(int telegramId);
}

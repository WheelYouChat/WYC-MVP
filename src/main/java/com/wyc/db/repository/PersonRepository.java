package com.wyc.db.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.Person;

@Repository
public interface PersonRepository extends PagingAndSortingRepository<Person, Integer>{
	List<Person> findByCarNumber(String carNumber);
}

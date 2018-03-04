package com.wyc.db.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.Car;

@Repository
public interface CarRepository extends PagingAndSortingRepository<Car, String>{
	List<Car> findByNumber(String number);
}

package com.wyc.db.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.AVICar;

@Repository
public interface AVICarRepository extends PagingAndSortingRepository<AVICar, String>{
	List<AVICar> findByNumberOrderByCreationDate(String number);
}

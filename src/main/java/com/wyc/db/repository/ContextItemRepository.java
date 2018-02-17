package com.wyc.db.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.ContextItem;

@Repository
public interface ContextItemRepository extends PagingAndSortingRepository<ContextItem, String>{
	List<ContextItem> findByPersonContextId(String personCotextId);
}

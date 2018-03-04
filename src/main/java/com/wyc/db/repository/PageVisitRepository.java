package com.wyc.db.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.Car;
import com.wyc.db.model.PageVisit;

@Repository
public interface PageVisitRepository extends PagingAndSortingRepository<PageVisit, Long>{
}

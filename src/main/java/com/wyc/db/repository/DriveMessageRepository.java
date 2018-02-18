package com.wyc.db.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.wyc.db.model.DriveMessage;

public interface DriveMessageRepository extends PagingAndSortingRepository<DriveMessage, String>{
	List<DriveMessage> findByToId(Integer toId);

	Iterable<DriveMessage> findByDeliveredDateIsNullOrderByIdDesc();
	List<DriveMessage> findByLocationTitleIsNullAndLongitudeIsNotNull();
}

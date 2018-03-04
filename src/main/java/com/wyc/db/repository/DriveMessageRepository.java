package com.wyc.db.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.wyc.db.model.DriveMessage;

public interface DriveMessageRepository extends PagingAndSortingRepository<DriveMessage, String>{
	Iterable<DriveMessage> findByDeliveredIsFalseOrderByIdDesc();
	List<DriveMessage> findByLocationTitleIsNullAndLongitudeIsNotNull();
	List<DriveMessage> findByRepliedToId(Long messageId);
}

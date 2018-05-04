package com.wyc.db.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessage.DriveMessageType;
import com.wyc.db.model.Person;

@Repository
public interface DriveMessageRepository extends PagingAndSortingRepository<DriveMessage, Long>{
	Iterable<DriveMessage> findByDeliveredIsFalseOrderByIdDesc();
	Iterable<DriveMessage> findByDeliveredIsFalseAndCreationDateGreaterThanOrderByIdDesc(Date creaitionDate);
	List<DriveMessage> findByLocationTitleIsNullAndLongitudeIsNotNull();
	List<DriveMessage> findByRepliedToId(Long messageId);
	List<DriveMessage> findByFromRole(Person.Role role);
	List<DriveMessage> findByMessageType(DriveMessageType type);
}

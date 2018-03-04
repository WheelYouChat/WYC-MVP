package com.wyc.db.repository;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.DriveMessageDelivery;

public interface DriveMessageDeliveryRepository extends PagingAndSortingRepository<DriveMessageDelivery, String>{
	Optional<DriveMessageDelivery> findByCode(String code);
	Optional<DriveMessageDelivery> findBySentMessageId(Integer sentMessageId);
}

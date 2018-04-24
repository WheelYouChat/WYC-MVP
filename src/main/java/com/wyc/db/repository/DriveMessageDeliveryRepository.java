package com.wyc.db.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.DriveMessageDelivery;
import com.wyc.db.model.DriveMessageDelivery.DeliveryType;

@Repository
public interface DriveMessageDeliveryRepository extends PagingAndSortingRepository<DriveMessageDelivery, String>{
	Optional<DriveMessageDelivery> findByCode(String code);
	Optional<DriveMessageDelivery> findBySentMessageId(String sentMessageId);
	List<DriveMessageDelivery> findByPhoneNumberOrderBySentDate(String phoneNumber);
	List<DriveMessageDelivery> findByCompletedAndDeliveryType(boolean completed, DeliveryType deliveryType);
	
}

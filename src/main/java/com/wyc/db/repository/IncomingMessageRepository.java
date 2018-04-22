package com.wyc.db.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.IncomingMessage;

@Repository
public interface IncomingMessageRepository extends PagingAndSortingRepository<IncomingMessage, Integer>{
	List<IncomingMessage> findByMessageId(String messageId);
}

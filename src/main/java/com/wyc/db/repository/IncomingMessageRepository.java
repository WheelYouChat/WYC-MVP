package com.wyc.db.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.IncomingMessage;

@Repository
public interface IncomingMessageRepository extends PagingAndSortingRepository<IncomingMessage, Integer>{
}

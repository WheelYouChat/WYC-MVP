package com.wyc.db.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.wyc.db.model.FeedbackMessage;

@Repository
public interface FeedbackMessageRepository  extends PagingAndSortingRepository<FeedbackMessage, Long>{

}

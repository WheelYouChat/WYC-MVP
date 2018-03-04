package com.wyc.service;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wyc.db.model.PageVisit;
import com.wyc.db.repository.PageVisitRepository;

@Service
public class PageVisitService {
	@Autowired
	private PageVisitRepository pageVisitRepository;
	
	public void logPageVisit(String type, HttpServletRequest request) {
		PageVisit pageVisit = PageVisit.builder()
				.ip(request.getRemoteAddr())
				.type(type)
				.method(request.getMethod())
				.url(request.getRequestURI())
				.visitDate(new Date())
				.build();
		pageVisitRepository.save(pageVisit);
	}
}

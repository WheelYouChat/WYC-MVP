package com.wyc.db.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextItem {

	@Id
	private String id;
	
	private String value;
	
	private int idx;
	
	@ManyToOne
	private PersonContext personContext;
}

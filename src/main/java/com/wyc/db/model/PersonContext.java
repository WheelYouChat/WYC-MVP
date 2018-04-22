package com.wyc.db.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonContext {
	@Id
	private String id;
	
	private String method;
	
	private Date creationDate;
	
	private Date lastActivityDate;
	
	@OneToMany(fetch=FetchType.EAGER)
	private List<ContextItem> items; 
	
	@OneToOne
	private Person person;
	
	private Integer lastMessageId;
}

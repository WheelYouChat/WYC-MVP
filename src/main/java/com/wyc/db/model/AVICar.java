package com.wyc.db.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Автомобиль из системы AVInfo

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AVICar {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	/**
	 * Госномер автомобиля
	 */
	private String number;

	/**
	 * Когда был создан
	 */
	private Date creationDate;

	private String phoneNumber;
	
	@Column(columnDefinition="TEXT")
	private String json;
	
}

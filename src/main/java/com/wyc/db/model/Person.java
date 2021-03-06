package com.wyc.db.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Person {
	
	@AllArgsConstructor
	@Getter
	public static enum Role {
		DRIVER("водитель"), 
		ADMIN("Администратор"), 
		FAKE("fake"), 
		PEDESTRIAN("пешеход");
		
		private final String title;
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	private Integer telegramId;
	
	private String viberId;
	
	private String carNumber;

	private String userName;
	
	private String phoneNumber;

	private String firstName;
	
	private String lastName;

	private String languageCode;
	
	private String avatar;
	
	private Date registrationDate;
	
	@Enumerated(EnumType.STRING)
	private Role role;
	
	private String nickname;
	
	private String carName;
	
	public String getUserDesc() {
		if(getNickname() == null && getCarName() == null) {
			return "Неизвестный";
		}
		if(getRole() == null || getRole() == Role.PEDESTRIAN) {
			return Role.PEDESTRIAN.getTitle() + " " + getNickname();
		}
		if(getNickname() != null && getCarName() == null) {
			return getNickname();
		}
		if(getNickname() == null && getCarName() != null) {
			return "Неизвестный - " + getCarName();
		}
		return getNickname() + " - " + getCarName();
	}
}

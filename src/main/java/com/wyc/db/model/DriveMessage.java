package com.wyc.db.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.wyc.chat.EnumMenu;
import com.wyc.chat.HasTitle;

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
public class DriveMessage {

	@Getter
	@AllArgsConstructor
	public static enum DriveMessageType implements HasTitle, EnumMenu {
		//                   123456789012345678901234567890
		
		// Ответы на жалобы
		SORRY              ("Извините"),
		SORRY_HURRY        ("Извините, очень спешил"),
		I_WAS_NOT_THERE    ("Вы ошиблись, меня там не было"),
		CAR_IS_NOT_THERE   ("Вы ошиблись, машина в другом месте"),
		APOLOGY_ACCEPTED   ("Извинения приняты"),
		APOLOGY_ACCEPTED_BUT_IT_WAS_DANGEROUS ("Принято, но это был опасный маневр"),
		PLEASE             ("Пожалуйста"),
		I_DONT_HAVE_TO_GIVE_WAY("Я не обязан был уступать"),
		
		//                   123456789012345678901234567890
		// Жалобы
		CUT_OFF            ("Вы меня подрезали",         "Вы меня подрезали", SORRY, SORRY_HURRY, I_WAS_NOT_THERE),
		CUT_OFF_SORRY      ("Извините,что подрезал",     "Извините, что подрезал", APOLOGY_ACCEPTED, APOLOGY_ACCEPTED_BUT_IT_WAS_DANGEROUS, I_WAS_NOT_THERE),
		THANKS_GIVE_WAY    ("Спасибо,что уступили",      "Спасибо, что уступили дорогу", PLEASE, I_WAS_NOT_THERE),
		DID_NOT_GIVE_WAY   ("Вы мне не уступили",        "Вы не уступили", SORRY, SORRY_HURRY, I_DONT_HAVE_TO_GIVE_WAY, I_WAS_NOT_THERE),
		WRONG_PARKING      ("Вы плохо запарковались",    "Ваша машина неправильно припаркована", SORRY, SORRY_HURRY, CAR_IS_NOT_THERE);

		DriveMessageType(String text) {
			this(text, text, new DriveMessageType[]{}, true);
		}
		
		DriveMessageType(String text, String sms) {
			this(text, sms, new DriveMessageType[]{}, true);
		}
		
		DriveMessageType(String text, String sms, DriveMessageType... answers) {
			this(text, sms, answers, false);
		}
		
		public String getName() {
			return name();
		}
		
		private final String sms;
		private final String title;
		private final DriveMessageType[] answers;
		
		/**
		 * Признак ответа (не жалоба)
		 */
		private final boolean answer;

		@Override
		public boolean isRootMenu() {
			return !answer;
		}
	}
	

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	private Person from;
	
	@ManyToOne
	private Person to;
	
	private String carNumberTo;
	
	private String message;

	private String smsMessage;

	@Enumerated(EnumType.STRING)
	private DriveMessageType messageType;
	
	private Date creationDate;
	
	// private Date sentDate;
	
	private Float longitude;

	private Float latitude;
	
	private String locationTitle;
	
	private boolean delivered;
	
	/**
	 * Сообщение на которое это сообщение отвечает
	 */
	@ManyToOne
	private DriveMessage repliedTo;
	
}

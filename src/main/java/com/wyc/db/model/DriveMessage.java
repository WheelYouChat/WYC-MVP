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
import com.wyc.chat.HasColor;
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
	public static enum DriveMessageType implements HasTitle, HasColor, EnumMenu {
		//                   123456789012345678901234567890
		
		// Ответы на жалобы
		SORRY              ("Извините", "Извините", GREEN),
		SORRY_HURRY        ("Извините, очень спешил", "Извините, очень спешил", GREEN),
		I_WAS_NOT_THERE    ("Вы ошиблись, меня там не было", "Вы ошиблись, меня там не было", BLUE),
		CAR_IS_NOT_THERE   ("Вы ошиблись, машина в другом месте", "Вы ошиблись, машина в другом месте", BLUE),
		APOLOGY_ACCEPTED   ("Извинения приняты", "Извинения приняты", GREEN),
		APOLOGY_ACCEPTED_BUT_IT_WAS_DANGEROUS ("Принято, но это был опасный маневр", "Принято, но это был опасный маневр", GREEN),
		PLEASE             ("Пожалуйста", "Пожалуйста", GREEN),
		I_DONT_HAVE_TO_GIVE_WAY("Я не обязан был уступать", "Я не обязан был уступать", RED),
		
		//                   123456789012345678901234567890
		// Жалобы
		CUT_OFF            ("Вы меня подрезали",         "Вы меня подрезали", RED, SORRY, SORRY_HURRY, I_WAS_NOT_THERE),
		CUT_OFF_SORRY      ("Извините,что подрезал",     "Извините, что подрезал", GREEN, APOLOGY_ACCEPTED, APOLOGY_ACCEPTED_BUT_IT_WAS_DANGEROUS, I_WAS_NOT_THERE),
		THANKS_GIVE_WAY    ("Спасибо,за проезд",         "Спасибо, что уступили дорогу", GREEN, PLEASE, I_WAS_NOT_THERE),
		DANGEROUS_DRIVING  ("Вы опасно ехали",           "Вы создавали опасные ситуации на дороге для других водителей", RED, SORRY, SORRY_HURRY, I_WAS_NOT_THERE),
		BROKE_RULES        ("Вы нарушили ПДД",           "Вы нарушили правила дорожного движения", RED, SORRY, SORRY_HURRY, I_WAS_NOT_THERE),
		DID_NOT_GIVE_WAY   ("Вы не уступили дорогу",     "Вы мне не уступили дорогу", RED, SORRY, SORRY_HURRY, I_DONT_HAVE_TO_GIVE_WAY, I_WAS_NOT_THERE),
		WRONG_PARKING      ("Вы плохо припаркованы",     "Ваша машина неправильно припаркована", RED, SORRY, SORRY_HURRY, CAR_IS_NOT_THERE),
		BLOCK_PARKING      ("Вы меня блокировали",       "Ваша машина заблокировала мне выезд", RED, SORRY, SORRY_HURRY, CAR_IS_NOT_THERE);
                                      
		DriveMessageType(String text) {
			this(text, text, new DriveMessageType[]{}, true, null);
		}
		
		DriveMessageType(String text, String sms) {
			this(text, sms, new DriveMessageType[]{}, true, null);
		}
		
		DriveMessageType(String text, String sms, DriveMessageType... answers) {
			this(text, sms, answers, false, null);
		}
		
		DriveMessageType(String text, String sms, String color) {
			this(text, sms, new DriveMessageType[]{}, true, color);
		}
		
		DriveMessageType(String text, String sms, String color, DriveMessageType... answers) {
			this(text, sms, answers, false, color);
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
		
		private final String color;

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

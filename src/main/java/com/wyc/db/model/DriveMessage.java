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
import com.wyc.db.model.Person.Role;

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
	public static enum DriveMessageType implements HasTitle, HasColor, EnumMenu, HasRoles {
		//                   123456789012345678901234567890
		
		// Ответы на жалобы
		SORRY              ("Извините", "Извините", GREEN, new Person.Role[0]),
		SORRY_USUALLY_DO   ("Извините, обычно я так и делаю", "Извините, обычно я так и делаю", GREEN, new Person.Role[0]),
		SORRY_USUALLY_DONT ("Извините, обычно я так не делаю", "Извините, обычно я так не делаю", GREEN, new Person.Role[0]),
		SORRY_HURRY        ("Извините, очень спешил", "Извините, очень спешил", GREEN, new Person.Role[0]),
		SORRY_TRAFFIC      ("Извините, была пробка и не куда было деваться", "Извините, была пробка и не куда было деваться", GREEN, new Person.Role[0]),
		SORRY_I_HAD_REASON ("Извините, но у меня были серьезные причины так поступить", "Извините, но у меня были серьезные причины так поступить", YELLOW, new Person.Role[0]),
		I_WAS_NOT_THERE    ("Вы ошиблись, меня там не было", "Вы ошиблись, меня там не было", BLUE, new Person.Role[0]),
		CAR_IS_NOT_THERE   ("Вы ошиблись, машина в другом месте", "Вы ошиблись, машина в другом месте", BLUE, new Person.Role[0]),
		APOLOGY_ACCEPTED   ("Извинения приняты", "Извинения приняты", GREEN, new Person.Role[0]),
		APOLOGY_ACCEPTED_BUT_IT_WAS_DANGEROUS ("Принято, но это был опасный маневр", "Принято, но это был опасный маневр", GREEN, new Person.Role[0]),
		PLEASE             ("Пожалуйста", "Пожалуйста", GREEN, new Person.Role[0]),
		I_DONT_HAVE_TO_GIVE_WAY("Я не обязан был уступать", "Я не обязан был уступать", RED, new Person.Role[0]),
		GLAD_MUSIC_IS_OK     ("Рад, что вам понравилось", "Рад, что вам понравилось", GREEN, new Person.Role[0]),
		
		//                     123456789012345678901234567890
		// Жалобы от водителей (иногда пешеходов
		THANKS_GIVE_WAY      ("Спасибо,за проезд",         "Спасибо, что уступили дорогу", GREEN, new Person.Role[]{Role.DRIVER, Role.ADMIN}, PLEASE, I_WAS_NOT_THERE),
		CUT_OFF_SORRY        ("Извините,что подрезал",     "Извините, что подрезал", GREEN, new Person.Role[]{Role.DRIVER, Role.ADMIN}, APOLOGY_ACCEPTED, APOLOGY_ACCEPTED_BUT_IT_WAS_DANGEROUS, I_WAS_NOT_THERE),
		GOOD_MUSIC           ("Класс.песня из вашего авто","Мне понравилась музыка, которая звучала из вашего автомобиля", GREEN, new Person.Role[]{Role.DRIVER, Role.ADMIN, Role.PEDESTRIAN}, GLAD_MUSIC_IS_OK, I_WAS_NOT_THERE),

		CUT_OFF              ("Вы меня подрезали",         "Вы меня подрезали", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, I_WAS_NOT_THERE),
		TURN_RIGHT_WRONG_LANE("Вы повернули вторым рядом", "Вы нарушили ПДД, повернув направо из неправильного ряда", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, I_WAS_NOT_THERE),
		TURN_LEFT_WRONG_LANE ("Вы повернули вторым рядом", "Вы нарушили ПДД, повернув налево из неправильного ряда", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, I_WAS_NOT_THERE),
		DANGEROUS_DRIVING    ("Вы опасно ехали",           "Вы создавали опасные ситуации на дороге для других водителей", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN, Role.PEDESTRIAN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, I_WAS_NOT_THERE),
		BROKE_RULES          ("Вы нарушили ПДД",           "Вы нарушили правила дорожного движения", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN, Role.PEDESTRIAN}, SORRY, SORRY_USUALLY_DONT,SORRY_HURRY, I_WAS_NOT_THERE),
		DID_NOT_GIVE_WAY     ("Вы не уступили дорогу",     "Вы мне не уступили дорогу", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN}, SORRY, SORRY_HURRY, I_DONT_HAVE_TO_GIVE_WAY, I_WAS_NOT_THERE),
		WRONG_PARKING        ("Вы плохо припаркованы",     "Ваша машина неправильно припаркована", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN, Role.PEDESTRIAN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, CAR_IS_NOT_THERE),
		BLOCK_PARKING        ("Вы меня блокировали",       "Ваша машина заблокировала мне выезд", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, CAR_IS_NOT_THERE),
		LOUD_SIGNAL          ("Вы громко сигналили",       "Вы очень громко сигналили, без явной необходимости", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN, Role.PEDESTRIAN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, I_WAS_NOT_THERE),
		USE_TURN_LIGHT       ("Включайте поворотник",      "Вы не использовали указатель поворота при перестроении или повороте, прошу используйте его", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN, Role.PEDESTRIAN}, SORRY, SORRY_USUALLY_DONT, I_WAS_NOT_THERE),
		USE_TURN_LIGHT_PARK  ("Включайте поворотник",      "Вы парковались и не использовали указатель поворотов, прошу используйте его когда ищете парковку или паркуетесь", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN, Role.PEDESTRIAN}, SORRY, SORRY_USUALLY_DONT, I_WAS_NOT_THERE),
		DONT_BLOCK_VIEW_180  ("Вы блокировали мне обзор",  "Мы разворачивались и вы встали справа от меня вторым рядом, заблокировав мне обзор. Пожалуйста, не делайте так, это мешает другим участникам движения.", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, I_WAS_NOT_THERE),
		DONT_BLOCK_VIEW_RIGHT("Вы блокировали мне обзор",  "Мы поворачивали направо и вы встали слева от меня вторым рядом, заблокировав мне обзор. Пожалуйста, не делайте так, это мешает другим участникам движения.", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, I_WAS_NOT_THERE),
		DONT_BLOCK_VIEW_LEFT ("Вы блокировали мне обзор",  "Мы поворачивали налево и вы встали справа от меня вторым рядом, заблокировав мне обзор. Пожалуйста, не делайте так, это мешает другим участникам движения.", RED, new Person.Role[]{Role.DRIVER, Role.ADMIN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, I_WAS_NOT_THERE),

		
		
		// Жалобы от пешеходов
		PED_DID_NOT_GIVE_WAY("Вы не пропустили пешехода",  "Вы не пропустили пешехода на пешеходном переходе", RED, new Person.Role[]{Role.PEDESTRIAN, Role.DRIVER, Role.ADMIN}, SORRY, SORRY_USUALLY_DONT, SORRY_HURRY, I_WAS_NOT_THERE),
		PED_BLOCK_CROSSWALK ("Блокировали переход",        "Вы блокировали пешеходный переход или проход пешеходов", RED, new Person.Role[]{Role.PEDESTRIAN, Role.DRIVER, Role.ADMIN}, SORRY, SORRY_USUALLY_DONT, SORRY_TRAFFIC, I_WAS_NOT_THERE),
		
		
		;

		DriveMessageType(String text, Person.Role[] roles) {
			this(text, text, roles, new DriveMessageType[]{}, true, null);
		}
		
		DriveMessageType(String text, String sms, Person.Role[] roles) {
			this(text, sms, roles, new DriveMessageType[]{}, true, null);
		}
		
		DriveMessageType(String text, String sms, Person.Role[] roles, DriveMessageType... answers) {
			this(text, sms, roles, answers, false, null);
		}
		
		DriveMessageType(String text, String sms, String color, Person.Role[] roles) {
			this(text, sms, roles, new DriveMessageType[]{}, true, color);
		}
		
		DriveMessageType(String text, String sms, String color, Person.Role[] roles, DriveMessageType... answers) {
			this(text, sms, roles, answers, false, color);
		}
		
		public String getName() {
			return name();
		}
		
		private final String sms;
		private final String title;
		private final Person.Role roles[];
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
		
		public DriveMessageType[] getAnswers() {
			return answers;
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

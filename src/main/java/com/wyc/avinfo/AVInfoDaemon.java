package com.wyc.avinfo;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.wyc.WYCConfig;
import com.wyc.chat.validator.PlateValidator;
import com.wyc.chat.validator.PlateValidator.Plate;
import com.wyc.db.model.AVICar;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.model.Person;
import com.wyc.db.repository.DriveMessageRepository;
import com.wyc.db.repository.PersonRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
/**
 * Демон находит послание, на которые не послали сообщения и делает запрос в AVInfo для получения телефона
 * @author ukman
 *
 */
public class AVInfoDaemon {
	
	@Autowired
	private WYCConfig wycConfig;
	
	@Autowired
	private DriveMessageRepository driveMessageRepository;
	
	@Autowired
	private PersonRepository personRepository;
	
	@Autowired
	private AVInfoService avinfoService;

	
	private volatile boolean processing = false; 
	
	/**
	 * Сканируем номера и пытаемся их найти в AVInfo
	 */
	@Scheduled(fixedRate=10000)
	public void scanCars() {
		if(wycConfig.isViberDelivery() && !processing) {
			log.debug("Scanning cars to request phones in AVInfo");
			try {
				processing = true;
				
				// Берем сообщения за последние сутки
				Date d = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
				Iterable<DriveMessage> messages = driveMessageRepository.findByDeliveredIsFalseAndCreationDateGreaterThanOrderByIdDesc(d);
				
				// Берем недоставленные сообщения
				for(DriveMessage message : messages) {
					String carNumber = message.getCarNumberTo();
					carNumber = carNumber.toUpperCase();
					if(acceptedToFind(carNumber)) {
						
						// Сначала проверяем, может такой человек уже есть с Viber account-ом
						List<Person> person = personRepository.findByCarNumber(carNumber);
						boolean personFound = person.stream().anyMatch(p -> {return StringUtils.isBlank(p.getViberId());});
						
						if(!personFound) {
							// Не нашли никого с заполненным ViberId
							log.debug("  Trying to get phone for " + carNumber);
							AVICar car = avinfoService.getCar(carNumber);
							log.debug("  found info for car number " + carNumber + " car = " + car);
						}
					}
				}
			} finally {
				processing = false;
			}
						
		}
	}

	/**
	 * Определяем, можно ли этот номер запрашивать в AVInfo. То есть определяем что номер из Мск или Моск. области
	 * @param number
	 * @return
	 */
	protected boolean acceptedToFind(String number) {
		Plate parsed = PlateValidator.parseNumber(number);
		boolean res = wycConfig.getAvinfo().getAreas().contains(parsed.getArea());
		return res;
	}
}

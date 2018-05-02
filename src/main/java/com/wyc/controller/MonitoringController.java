package com.wyc.controller;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wyc.WYCConfig;
import com.wyc.service.MonitoringService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/mntrng258")
@Slf4j
public class MonitoringController {

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	private Environment environment;
	
	@Autowired
	private MonitoringService monitoringService;
	
	@Autowired 
	private WYCConfig wycConfig;
	
	@RequestMapping("/cnfg")
	public WYCConfig getConfig() {
		WYCConfig res = WYCConfig.builder()
				.smsDelivery(wycConfig.isSmsDelivery())
				.viberDelivery(wycConfig.isViberDelivery())
				.webHook(wycConfig.isWebHook())
				.build();
		return res;
	}
	
	@RequestMapping("/ping")
	public Integer ping() {
		return 0;
	}
	
	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ApplicationInfo {
		private String appName;
		private String[] profiles;
	}
	@RequestMapping("/info")
	public ApplicationInfo info() {
		return ApplicationInfo.builder()
				.appName(this.appName)
				.profiles(environment.getActiveProfiles())
				.build();
	}
	
	@RequestMapping("/gitstatus")
	public List<String> getGitStatus() throws IOException, InterruptedException {
		getCmdOutput("git fetch");
		return getCmdOutput("git status");
	}
	
	@RequestMapping("/gitlog")
	public List<String> getGitLog() throws IOException, InterruptedException {
		return getCmdOutput("git log");
	}
	
	protected List<String> getCmdOutput(String cmd) throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec(cmd);
		InputStream is = p.getInputStream();
		p.waitFor();
		List<String> res = IOUtils.readLines(is);
		return res;
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Funnel {
		// Сколько всего было создано первых сообщений
		private int initialMessagesCreated;

		// Сколько номеров телефонов найдено
		private int phoneNumbersFound;
		
		// Сколько сообщений отослано
		private int smsMessagesSent;
		
		// Сколько сообщений доставлено
		private int smsMessagesDelivered;
		
		// Сколько заходов на ответную страницу сделано
		private int answerPageOpened;
		
		// Сколько ответов сделано с ответной страницы
		private int answerSentFromAnswerPage;
		
		// Сколько человек подписалось
		private int subscribed;
		
		// Сколько человек послали еще одно сообщение 
		private int activeSubscribers;
		
		public String getDescription() {
			return "Найдено - " + 
					(int)(100. * phoneNumbersFound / initialMessagesCreated)
				+ "% - послано " + (int)(100. * smsMessagesSent / initialMessagesCreated)
				+ "% - доставлено " + (int)(100. * smsMessagesDelivered / initialMessagesCreated)
				+ "% - открыто " + (int)(100. * answerPageOpened / initialMessagesCreated)
				+ "% - ответов " + (int)(100. * answerSentFromAnswerPage / initialMessagesCreated)
				+ "% - подписалось " + (int)(100. * subscribed / initialMessagesCreated)
				+ "% - вовлеклось " + (int)(100. * activeSubscribers / initialMessagesCreated) + "%"
				;
		}
	}
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@RequestMapping("/funnel")
	public Funnel getFunnel(@RequestParam(name="date", required=false, defaultValue="") String sFirstDate) throws ParseException {
		Date firstDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
		if(sFirstDate != null && sFirstDate.trim().length() > 0) {
			firstDate = new SimpleDateFormat("yyyy-MM-dd").parse(sFirstDate);
		}
		log.info("getFunnel firstDate = " + firstDate);
		Funnel funnel = new Funnel();
		funnel.setInitialMessagesCreated(jdbcTemplate.queryForObject(""
				+ "SELECT count(*) "
				+ "FROM ( "
				+ "  SELECT DISTINCT car_number_to "
				+ "  FROM drive_message WHERE car_number_to IS NOT NULL AND creation_date >= ?"
				+ ") AS t", new Object[]{firstDate}, Integer.class));
		
		funnel.setPhoneNumbersFound(jdbcTemplate.queryForObject(""
				+ "SELECT count(*) FROM avicar WHERE creation_date  >= ?", new Object[]{firstDate}, Integer.class));
		
		funnel.setSmsMessagesSent(jdbcTemplate.queryForObject(""
				+ "SELECT count(*) FROM drive_message_delivery WHERE delivery_type = 'SMS' AND sent_date >= ?", new Object[]{firstDate}, Integer.class));
		
		funnel.setSmsMessagesDelivered(jdbcTemplate.queryForObject(""
				+ "SELECT count(*) FROM drive_message_delivery WHERE delivery_type = 'SMS' AND delivery_status = 'DELIVERED' AND sent_date >= ?", new Object[]{firstDate}, Integer.class));
		
		funnel.setAnswerPageOpened(jdbcTemplate.queryForObject(""
				+ "SELECT count(*) FROM ("
				+ "  SELECT DISTINCT code "
				+ "  FROM drive_message_delivery dmd JOIN page_visit pv ON concat('/', dmd.code) = pv.url "
				+ "  WHERE delivery_type = 'SMS' AND delivery_status = 'DELIVERED' AND sent_date >= ?"
				+ ") AS t", new Object[]{firstDate}, Integer.class));
		funnel.setAnswerSentFromAnswerPage(jdbcTemplate.queryForObject(""
				+ "SELECT count(*) FROM ("
				+ "  SELECT DISTINCT code "
				+ "  FROM drive_message_delivery dmd JOIN page_visit pv ON concat('/', dmd.code) = pv.url "
				+ "    JOIN drive_message dm ON dm.replied_to_id = dmd.id"
				+ "  WHERE delivery_type = 'SMS' AND delivery_status = 'DELIVERED' AND sent_date >= ?"
				+ ") AS t", new Object[]{firstDate}, Integer.class));
		
		funnel.setSubscribed(jdbcTemplate.queryForObject("SELECT count(*) FROM person WHERE registration_date >= ?", new Object[]{firstDate}, Integer.class));
		funnel.setActiveSubscribers(jdbcTemplate.queryForObject(""
				+ "SELECT count(*) FROM ( "
				+ "  SELECT DISTINCT p.id "
				+ "  FROM person p JOIN drive_message dm ON p.id = dm.from_id "
				+ "  WHERE registration_date > ?"
				+ ") AS t", new Object[]{firstDate}, Integer.class));
		return funnel;
	}

	@Data
	@ToString
	@Builder
	public static class MonitoringInfo {
		public static enum State {
			OK, ERROR
		}
		
		private String title;
		
		private State state;
		
		private String message;
	}
	
	@RequestMapping("monitor")
	public MonitoringInfo[] monitor() {
		MonitoringInfo[] res = monitoringService.getMonitorInfos();
		return res;
	}
}

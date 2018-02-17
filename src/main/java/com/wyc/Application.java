package com.wyc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
@EnableScheduling
public class Application {
	public static void main(String agrs[]) {
		ApiContextInitializer.init();
		SpringApplication.run(Application.class);
    }
}

package com.nick.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;


@SpringBootApplication
public class MyBotApplication {

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(MyBotApplication.class, args);
		var myBot = context.getBean(MyBot.class);
		String token = context.getEnvironment().getProperty("bot.token");

		try(TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
			botsApplication.registerBot(token, myBot);
			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

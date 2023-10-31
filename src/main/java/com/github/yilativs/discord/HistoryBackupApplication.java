package com.github.yilativs.discord;

import java.util.SortedSet;

import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.yilativs.discord.model.Message;

public class HistoryBackupApplication {
	private static final Logger logger = LoggerFactory.getLogger(HistoryBackupApplication.class);
	private static final int MINUTES_TO_WAIT_FOR_RECONNECT_TO_WEB_DRIVER = 1;
	private static final int ATTEMPTS_TO_RECONNECT_TO_WEB_DRIVER = 30;

	private DiscordService discordService;

	public static void main(String[] args) {
		HistoryBackupApplication application = new HistoryBackupApplication(new DiscordService());
		addShutdownHookToDisposeWebDriver(application);
		try {
			application.startBackup();
		} catch (InterruptedException e) {
			logger.info("execution was interrupted, stopping tracking activity");
		}
	}

	public HistoryBackupApplication(DiscordService discordService) {
		this.discordService = discordService;
	}

	private static void addShutdownHookToDisposeWebDriver(final HistoryBackupApplication activiyTracker) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("backup was stopped, flushing data.");
		}));
	}

	private void startBackup() throws InterruptedException {
		int attemptCount = 0;
		long backupStart = System.currentTimeMillis();
		while (ATTEMPTS_TO_RECONNECT_TO_WEB_DRIVER - attemptCount > 0) {
			attemptCount++;
			try {
				discordService.login();
				discordService.clickChannel(discordService.getAccount());
				do {
					SortedSet<Message> messages = discordService.getMessages(backupStart, 100);
					if (!messages.isEmpty() && backupStart != messages.getLast().unixTimestamp()) {
						save(messages);
						backupStart = messages.getFirst().unixTimestamp();
					} else {
						logger.info("backup completed");
						return;
					}

				} while (true);
			} catch (WebDriverException e) {
				logger.error("attempt: " + attemptCount + " failed because of: " + e.getMessage(), e);
				Thread.sleep(1000 * 60 * MINUTES_TO_WAIT_FOR_RECONNECT_TO_WEB_DRIVER);
			}
		}
	}

	private void save(SortedSet<Message> messages) {
		for (Message message : messages) {
			System.out.println(message);
		}
		
	}

}
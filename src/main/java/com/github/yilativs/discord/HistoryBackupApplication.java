package com.github.yilativs.discord;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.SortedSet;

import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.yilativs.discord.model.Message;
import com.github.yilativs.discord.repository.MessageRepository;
import com.github.yilativs.discord.service.DiscordService;
import com.github.yilativs.discord.service.ExternalFileService;

public class HistoryBackupApplication {
	private static final int MESSAGES_BATCH_SIZE = 100;
	private static final Logger logger = LoggerFactory.getLogger(HistoryBackupApplication.class);
	private static final int MINUTES_TO_WAIT_FOR_RECONNECT_TO_WEB_DRIVER = 1;
	private static final int ATTEMPTS_TO_RECONNECT_TO_WEB_DRIVER = 30;

	private DiscordService discordService;
	private ExternalFileService externalFileService;
	private MessageRepository messageRepository;

	public static void main(String[] args) {
		DiscordService discordService = new DiscordService();
		ExternalFileService externalFileService = new ExternalFileService(discordService.getAccount());
		MessageRepository messageRepository = new MessageRepository(discordService.getDbUrl(),
				discordService.getDbUser(), discordService.getDbPassword());
		HistoryBackupApplication application = new HistoryBackupApplication(discordService, externalFileService,
				messageRepository);
		addShutdownHookToDisposeWebDriver(application);
		try {
			application.startBackup();
		} catch (InterruptedException e) {
			logger.info("execution was interrupted, stopping tracking activity");
		}
	}

	public HistoryBackupApplication(DiscordService discordService, ExternalFileService externalFileService,
			MessageRepository messageRepository) {
		this.discordService = discordService;
		this.externalFileService = externalFileService;
		this.messageRepository = messageRepository;
	}

	private static void addShutdownHookToDisposeWebDriver(final HistoryBackupApplication activiyTracker) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("backup was stopped, flushing data.");
		}));
	}

	private void startBackup() throws InterruptedException {
		int attemptCount = 0;
		// TODO validate if there is a need for shifting it to alight with GMT
		LocalDateTime toGmtLocalDateTime = LocalDateTime.now(ZoneOffset.UTC);
		while (ATTEMPTS_TO_RECONNECT_TO_WEB_DRIVER - attemptCount > 0) {
			attemptCount++;
			try {
				discordService.login();
				discordService.clickChannel(discordService.getAccount());
				do {
					SortedSet<Message> messages = discordService.getMessages(toGmtLocalDateTime, MESSAGES_BATCH_SIZE);
					if (!messages.isEmpty()) {
						save(messages);
						toGmtLocalDateTime = messages.getFirst().gmtDateTime();
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

	private void save(Collection<Message> messages) {
		externalFileService.save(messages);
		messageRepository.save(messages);
	}

}
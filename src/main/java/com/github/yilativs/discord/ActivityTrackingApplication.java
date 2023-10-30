package com.github.yilativs.discord;

import static java.nio.file.Files.write;
import static java.time.LocalDateTime.now;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityTrackingApplication {
	private static final Logger logger = LoggerFactory.getLogger(ActivityTrackingApplication.class);
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String ACTIVITY_LOG_FILE_NAME = "-activity.log";
	private static final int MINUTES_TO_WAIT_FOR_RECONNECT_TO_WEB_DRIVER = 1;
	private static final int ATTEMPTS_TO_RECONNECT_TO_WEB_DRIVER = 30;

	enum Status {
		ONLINE, OFFLINE
	}

	private static final int PAUSE_BETWEEN_ONLINE_CHECKS_IN_SECONDS = 5;
	private static String activityLogFileName;
	private LocalDateTime periodStartTimestamp = null;

	private DiscordService discordService;

	public static void main(String[] args) {
		ActivityTrackingApplication application = new ActivityTrackingApplication(new DiscordService());
		addShutdownHookToDisposeWebDriver(application);
		try {
			application.startTracking();
		} catch (InterruptedException e) {
			logger.info("execution was interrupted, stopping tracking activity");
		}
	}

	public ActivityTrackingApplication(DiscordService discordService) {
		this.discordService = discordService;
		activityLogFileName = createActivityLogFileIfMissing(discordService.getAccount());
	}

	public boolean isPeriodStarted() {
		return periodStartTimestamp != null;
	}

	private static void addShutdownHookToDisposeWebDriver(final ActivityTrackingApplication activiyTracker) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (activiyTracker.isPeriodStarted()) {
				activiyTracker.writeActivityTimeToLog(now(), Status.OFFLINE);
			}
			logger.info("tracking is stopped, flushing data to file.");
		}));
	}

	private void startTracking() throws InterruptedException {
		int attemptCount = 0;
		while (ATTEMPTS_TO_RECONNECT_TO_WEB_DRIVER - attemptCount > 0) {
			attemptCount++;
			try {
				discordService.login();
				while (true) {
					if (discordService.isOffline()) {
						if (periodStartTimestamp != null) {
							writeActivityTimeToLog(now(), Status.OFFLINE);
							periodStartTimestamp = null;
						}
					} else {
						if (periodStartTimestamp == null) {
							periodStartTimestamp = now();
							writeActivityTimeToLog(periodStartTimestamp, Status.ONLINE);
						}
					}
					Thread.sleep(PAUSE_BETWEEN_ONLINE_CHECKS_IN_SECONDS * 1000);
				}
			} catch (WebDriverException e) {
				logger.error("attempt: " + attemptCount + " failed because of: " + e.getMessage(), e);
				Thread.sleep(1000 * 60 * MINUTES_TO_WAIT_FOR_RECONNECT_TO_WEB_DRIVER);
			}
		}
	}

	private String createActivityLogFileIfMissing(String account) {
		String activityLogFileName = System.getProperty("user.dir") + FileSystems.getDefault().getSeparator() + account
				+ ACTIVITY_LOG_FILE_NAME;
		File activityLogFile = new File(activityLogFileName);
		if (!activityLogFile.exists()) {
			try {
				if (activityLogFile.createNewFile()) {
					logger.error("failed to create acitivy log file");
					System.exit(50);
				}
			} catch (IOException e) {
				logger.error("failed to create acitivy log file because " + e.getMessage());
				System.exit(50);
			}
		}
		return activityLogFileName;
	}

	private void writeActivityTimeToLog(LocalDateTime timestamp, Status status) {
		try {
			if (status == Status.ONLINE) {
				logger.info("online");
				write(Paths.get(activityLogFileName), (timestamp.format(FORMATTER) + "\t").getBytes(),
						StandardOpenOption.APPEND);
			} else {
				logger.info("offline");
				write(Paths.get(activityLogFileName), (timestamp.format(FORMATTER) + System.lineSeparator()).getBytes(),
						StandardOpenOption.APPEND);
			}

		} catch (IOException e) {
			logger.error("failed to write activity because " + e.getMessage());
		}
	}

}
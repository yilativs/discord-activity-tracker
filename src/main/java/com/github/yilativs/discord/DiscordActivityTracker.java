package com.github.yilativs.discord;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordActivityTracker {
	private static final String ACTIVITY_LOG_FILE_NAME = "-activity.log";
	private static final String EMAIL = "email";
	private static final String SEPARATOR = FileSystems.getDefault().getSeparator();
	private static final String USER_DIR = "user.dir";
	private static final String DISCORD_ACTIVITY_TRACKER = "discord-activity-tracker.properties";
	private static final Logger logger = LoggerFactory.getLogger(DiscordActivityTracker.class);
	private static final String ACCOUNT_TO_TRACK = "account-to-track";
	private static final String PASSWORD = "password";

	public static void main(String[] args) throws InterruptedException {
		logger.info("Discord Activity Tracker started");

		String currentDir = System.getProperty(USER_DIR);
		File currentDirPropertyFile = new File(currentDir + SEPARATOR + DISCORD_ACTIVITY_TRACKER);
		try (InputStream input = new FileInputStream(currentDirPropertyFile)) {
			Properties properties = new Properties();
			properties.load(input);
			String email = getProperty(properties, EMAIL);
			String account = getProperty(properties, ACCOUNT_TO_TRACK);
			String password = getPassword(properties);
			String activityLogFileName = createActivityLogFIleIfMissing(currentDir, account);

			ChromeDriver driver = login(email, password);
			addShutdownHookToDisposeWebDriver(driver);
			trackActivity(driver, account, activityLogFileName);
		} catch (FileNotFoundException e) {
			logger.error("Configuration file not found at:" + currentDirPropertyFile);
			System.exit(10);
		} catch (IOException e) {
			logger.error(
					"Failed to read configuration file at:" + currentDirPropertyFile + " beacause " + e.getMessage());
			System.exit(20);
		}

	}

	private static String getPassword(Properties properties) {
		String password = getProperty(properties, PASSWORD);
		if (password == null) {
			password = requestUserPassword();
		}
		return password;
	}

	private static String requestUserPassword() {
		System.out.print("password: ");
		String password = new String(System.console().readPassword());
		return password;
	}

	private static void addShutdownHookToDisposeWebDriver(final ChromeDriver driver) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			driver.quit();
			logger.info("tracking is stopped.");
		}));
	}

	private static ChromeDriver login(String email, String password) throws InterruptedException {
		ChromeDriver driver = createChromeDriver();
		driver.get("https://discord.com/channels/@me");
		Thread.sleep(2000);
		driver.findElement(By.name(EMAIL)).sendKeys(email);
		Thread.sleep(1000);
		driver.findElement(By.name("password")).sendKeys(password);
		var b = driver.findElement(By.xpath("//button[@type='submit']"));
		Thread.sleep(1000);
		b.click();
		Thread.sleep(10000);
		return driver;
	}

	private static String getProperty(Properties properties, String propertyName) {
		String propertyValue = properties.getProperty(propertyName);
		if (propertyValue == null || propertyValue.isBlank()) {
			logger.error("Missing property " + propertyName);
			System.exit(30);
		}
		return propertyValue;
	}

	private static ChromeDriver createChromeDriver() {
		System.setProperty("webdriver.chrome.driver", "/opt/webdriver/chromedriver");
		ChromeOptions options = new ChromeOptions();
		options.setBinary("/opt/google/chrome/chrome");
		options.addArguments("--disable-gpu");
		options.addArguments("--disable-crash-reporter");
		options.addArguments("--disable-extensions");
		options.addArguments("--disable-in-process-stack-traces");
		options.addArguments("--disable-logging");
		options.addArguments("--disable-dev-shm-usage");
//		options.addArguments("--log-level=3");
		options.addArguments("--output=/dev/null");
//		options.addArguments("--headless");
//		options.addArguments("--window-size=1920,1200");
		options.addArguments("--allow-running-insecure-content");
		options.addArguments("--ignore-certificate-errors");
		options.addArguments("--allow-running-insecure-content");
		options.addArguments("--no-sandbox");
		options.setExperimentalOption("prefs", Map.of("profile.managed_default_content_settings.images", 2));
		var d = new ChromeDriver(options);
		return d;
	}

	private static void trackActivity(ChromeDriver driver, String accountId, String activityLogFileName)
			throws InterruptedException {
		while (true) {
			// click on the tacked user icon and copy id
			var element = driver.findElement(By.xpath("//a[@href='/channels/@me/" + accountId + "']"));
			if (isNotGreyCircle(element)) {
				writeActivityTimeToLog(accountId, activityLogFileName);
			}
			Thread.sleep(30 * 1000);
		}
	}

	private static void writeActivityTimeToLog(String accountId, String activityLogFileName) {
		try {
			Files.write(Paths.get(activityLogFileName),
					(LocalDateTime.now().format(ISO_LOCAL_DATE_TIME) + System.lineSeparator()).getBytes(),
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			logger.error(
					"failed to write to " + accountId + ACTIVITY_LOG_FILE_NAME + " because " + e.getMessage());
		}
		logger.info("online");
	}

	private static boolean isNotGreyCircle(WebElement element) {
		return element.findElements(By.cssSelector("rect[fill='#747f8d']")).size() == 0;
	}

	private static String createActivityLogFIleIfMissing(String currentDir, String account) {
		String activityLogFileName = currentDir + SEPARATOR + account + ACTIVITY_LOG_FILE_NAME;
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

}

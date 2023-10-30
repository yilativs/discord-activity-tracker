package com.github.yilativs.discord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.Properties;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordService {
	private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);

	private static final String EMAIL = "email";

	private static final String PROPERTY_FILE_NAME = "discord.properties";

	private static final String ACCOUNT_TO_TRACK = "account-to-track";
	private static final String PASSWORD = "password";
	

	private String email;
	private String account;
	private String password;
	private static ChromeDriver driver;
	
	private static final String USER_DIR = "user.dir";

	public DiscordService() {
		logger.info("Discord Activity Tracker started");
		String currentDir = System.getProperty(USER_DIR);
		File currentDirPropertyFile = new File(
				currentDir + FileSystems.getDefault().getSeparator() + PROPERTY_FILE_NAME);

		try (InputStream input = new FileInputStream(currentDirPropertyFile)) {
			Properties properties = new Properties();
			properties.load(input);
			email = getProperty(properties, EMAIL);
			account = getProperty(properties, ACCOUNT_TO_TRACK);
			password = getPassword(properties);
			driver = createChromeDriver();
		} catch (FileNotFoundException e) {
			logger.error("Configuration file not found at:" + PROPERTY_FILE_NAME);
			System.exit(10);
		} catch (IOException e) {
			logger.error("Failed to read configuration file at:" + PROPERTY_FILE_NAME + " beacause " + e.getMessage());
			System.exit(20);
		}
	}

	private String getPassword(Properties properties) {
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
			if (driver != null)
				logger.info("tracking is stopped. closing driver");
			driver.quit();

		}));
	}

	public void login() throws InterruptedException {
		driver.get("https://discord.com/channels/@me");
		Thread.sleep(3000);
		driver.findElement(By.name(EMAIL)).sendKeys(email);
		Thread.sleep(1000);
		driver.findElement(By.name("password")).sendKeys(password);
		var b = driver.findElement(By.xpath("//button[@type='submit']"));
		Thread.sleep(1000);
		b.click();
		Thread.sleep(15000);
	}

	private static String getProperty(Properties properties, String propertyName) {
		String propertyValue = properties.getProperty(propertyName);
		if (propertyValue == null || propertyValue.isBlank()) {
			logger.error("Missing property " + propertyName);
			System.exit(10);
		}
		return propertyValue;
	}

	private static ChromeDriver createChromeDriver() {
		System.setProperty("webdriver.chrome.driver", "/opt/webdriver/chromedriver");
		ChromeOptions options = new ChromeOptions();
		options.setBinary("/opt/chrome/chrome");
//		options.addArguments("--headless=new");
		options.addArguments("--disable-gpu");
		options.addArguments("--disable-crash-reporter");
		options.addArguments("--disable-extensions");
		options.addArguments("--disable-in-process-stack-traces");
		options.addArguments("--disable-logging");
		options.addArguments("--disable-dev-shm-usage");
//		options.addArguments("--log-level=3");
		options.addArguments("--output=/dev/null");
//		options.addArguments("--window-size=1920,1200");
		options.addArguments("--allow-running-insecure-content");
		options.addArguments("--ignore-certificate-errors");

		options.addArguments("--no-sandbox");
//		options.setExperimentalOption("prefs", Map.of("profiBogdan Stojčićle.managed_default_content_settings.images", 2));
		// driver = WebDriverManager.chromedriver().create();
		var driver = new ChromeDriver(options);
		addShutdownHookToDisposeWebDriver(driver);
		return driver;
	}

	public boolean isOffline() throws InterruptedException {
		try {
			// click on the tacked user icon and copy id
			WebElement element = driver.findElement(By.xpath("//a[@href='/channels/@me/" + account + "']"));
			String ariaLabel = element.findElement(By.className("wrapper_edb6e0")).getAttribute("aria-label").toLowerCase();
			return ariaLabel.contains("offline");
		} catch (WebDriverException e) {
			driver.close();
			throw e;
		}
	}

	public String getAccount() {
		return account;
	}

}

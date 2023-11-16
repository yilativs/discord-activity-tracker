package com.github.yilativs.discord.service;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.tagName;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.yilativs.discord.model.Emoji;
import com.github.yilativs.discord.model.ExternalFile;
import com.github.yilativs.discord.model.Message;

public class DiscordService {
	private static final int PAGE_UP_COUNT = 50;

	private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);

	private static final String EMAIL = "email";

	private static final String PROPERTY_FILE_NAME = "discord.properties";

	private static final String ACCOUNT_TO_TRACK = "account-to-track";
	private static final String PASSWORD = "password";

	private String email;
	private String account;
	private String password;

	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	private static ChromeDriver driver;

	private static final String USER_DIR = "user.dir";

	private Robot robot;

	public String getDbUrl() {
		return dbUrl;
	}

	public String getDbUser() {
		return dbUser;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public DiscordService() {
		logger.info("Discord Activity Tracker started");
		String currentDir = System.getProperty(USER_DIR);
		File currentDirPropertyFile = new File(
				currentDir + FileSystems.getDefault().getSeparator() + PROPERTY_FILE_NAME);
		try {
			robot = new Robot();
		} catch (AWTException e) {
			logger.error("Robot creation failed", e);
		}

		try (InputStream input = new FileInputStream(currentDirPropertyFile)) {
			Properties properties = new Properties();
			properties.load(input);
			email = getProperty(properties, EMAIL);
			account = getProperty(properties, ACCOUNT_TO_TRACK);
			password = getPassword(properties);

			dbUrl = getProperty(properties, "db-url");
			dbUser = getProperty(properties, "db-user");
			// TODO replace with secured password reading
			dbPassword = getProperty(properties, "db-password");
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

	public void clickChannel(String id) throws InterruptedException {
		driver.get("https://discord.com/channels/@me/" + id);
//		driver.findElement(By.cssSelector("a[data-list-item-id='private-channels-uid_26___" + id + "']")).click();
		Thread.sleep(3000);
	}

	/**
	 * 
	 * @param toGmtDateTime only messages before this timestamp will be returned
	 * @param softSizeLimit the amount of returned values may be a bit bigger than
	 *                      softSizeLimit
	 * @return messages
	 * @throws InterruptedException
	 */
	public SortedSet<Message> getMessages(LocalDateTime toGmtDateTime, int softSizeLimit) throws InterruptedException {
		// we want it reversed, so that oldest messages were first
		SortedSet<Message> messages = new TreeSet<Message>((a, b) -> b.gmtDateTime().compareTo(a.gmtDateTime()));
		do {
			List<Message> pageMessages = getPageMessages(toGmtDateTime);
			messages.addAll(pageMessages);
			if (pageMessages.isEmpty()) {
				break;
			}
			messages.addAll(pageMessages);
			toGmtDateTime = pageMessages.getFirst().gmtDateTime();
			pressPageUpNTimes();
		} while (messages.size() < softSizeLimit);
		return messages;
	}

	private void pressPageUpNTimes() throws InterruptedException {
		for (int i = 0; i < PAGE_UP_COUNT; i++) {
			pressPageUp();
		}
		Thread.sleep(1000);
	}

	private void pressPageUp() throws InterruptedException {
		robot.keyPress(KeyEvent.VK_PAGE_UP);
		robot.keyRelease(KeyEvent.VK_PAGE_UP);
		Thread.sleep(500);
	}

	private List<Message> getPageMessages(LocalDateTime toGmtDateTime) {
		List<Message> messages = new ArrayList<>();
		List<WebElement> webElements = driver.findElements(className("messageListItem__6a4fb"));
		String avatarUrl = null;
		String userName = null;
		for (WebElement element : webElements) {
			try {
				LocalDateTime gmtDateTime = getDateTime(element);
				if (toGmtDateTime.isBefore(gmtDateTime)) {
					break;
				}
				Optional<String> messageUserName = getUserName(element);
				userName = messageUserName.isPresent() ? messageUserName.get() : userName;
				// returns previos avatar if not found
				avatarUrl = getAvatarUrl(element, avatarUrl);
				String replyText = getReplyText(element);
				String text = getText(element);
				List<Emoji> emojes = getEmojes(element);
				ExternalFile image = getMessageImage(element);
				ExternalFile video = getMessageVideo(element);
				messages.add(new Message(userName, gmtDateTime, avatarUrl, replyText, video, image, emojes, text));
			} catch (RuntimeException e) {
				logger.error("failed to read message because of :" + e.getMessage(), e);
			}
		}
		return messages;
	}

	private ExternalFile getMessageVideo(WebElement element) {
		List<WebElement> videos = element.findElements(By.cssSelector("video[class='video__4c052'"));
		return videos.isEmpty() ? null
				: new ExternalFile(videos.getFirst().getAttribute("src"), videos.getFirst().getAttribute("poster"));
	}

	private Optional<String> getUserName(WebElement element) {
		// <span class="username_d30d99 desaturateUserColors_b72bd3 clickable_d866f1"
		// aria-expanded="false" role="button" tabindex="0">Vitaliy</span>
		var elements = element.findElements(className("username_d30d99"));
		return elements.isEmpty() ? empty() : of(elements.getFirst().getText());
	}

	private ExternalFile getMessageImage(WebElement element) {
		List<WebElement> images = element.findElements(By.cssSelector("a[data-role='img'"));
		return images.isEmpty() ? null
				: new ExternalFile(images.getFirst().getAttribute("href"), images.getFirst().getAttribute("data-safe-src"));
	}

	private List<Emoji> getEmojes(WebElement element) {
		return element.findElements(By.cssSelector("img[class='emoji'")).stream()
				.map(e -> new Emoji(e.getAttribute("src"), e.getAttribute("alt"))).toList();
	}

	private String getText(WebElement element) {
		return element.findElement(className("messageContent__21e69")).getText();
	}

	private String getReplyText(WebElement element) {
		var elements = element.findElements(className("repliedTextPreview__90311"));// clickable_d866f1
		return elements.isEmpty() ? null : elements.getFirst().getText();
	}

	private String getAvatarUrl(WebElement element, String previosAvatarUrl) {
		List<WebElement> avatarElements = element.findElements(className("avatar__08316"));// clickable_d866f1
		return avatarElements.isEmpty() ? previosAvatarUrl : avatarElements.getFirst().getAttribute("src");
	}

	private LocalDateTime getDateTime(WebElement element) {
		// TODO replace with getting LocalDateTime
		// <time aria-label="October 18, 2023 12:32 PM"
		// id="message-timestamp-1164148929085067324"
		// datetime="2023-10-18T10:32:13.535Z"
		CharSequence dateTimeString = element.findElement(tagName("time")).getAttribute("datetime");
		return LocalDateTime.parse(dateTimeString.subSequence(0, dateTimeString.length() - 1));
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
			String ariaLabel = element.findElement(className("wrapper_edb6e0")).getAttribute("aria-label")
					.toLowerCase();
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

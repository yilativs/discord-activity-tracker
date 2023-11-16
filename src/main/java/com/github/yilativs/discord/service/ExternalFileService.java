package com.github.yilativs.discord.service;

import static com.github.yilativs.discord.service.ExternalFileService.FileSystemLimit.FILE;
import static java.io.File.separator;
import static java.lang.System.getProperty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.yilativs.discord.model.ExternalFile;
import com.github.yilativs.discord.model.Message;

public class ExternalFileService {

	enum FileSystemLimit {
		FILE(255), DIR(4096);

		final int length;

		private FileSystemLimit(int length) {
			this.length = length;
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(ExternalFileService.class);

	private static final String USER_AGENT = " Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/119.0";

	private final String filesDirPath;

	public ExternalFileService(String accountToTrack) {
		filesDirPath = getProperty("user.dir") + separator + "files" + separator + accountToTrack;

	}

	public void save(Collection<Message> messages) {
		createUrlToFile(getListOfImages(messages)).forEach((url, file) -> save(url, file));
		createUrlToFile(getListOfVideos(messages)).forEach((url, file) -> save(url, file));
	}

	private List<ExternalFile> getListOfImages(Collection<Message> messages) {
		return messages.stream().filter(m -> m.image() != null).map(m -> m.image()).toList();
	}

	private List<ExternalFile> getListOfVideos(Collection<Message> messages) {
		return messages.stream().filter(m -> m.video() != null).map(m -> m.video()).toList();
	}

	void save(URL url, File file) {
		try {
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", USER_AGENT);
			ReadableByteChannel readableByteChannel = Channels.newChannel(connection.getInputStream());
			try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
				FileChannel fileChannel = fileOutputStream.getChannel();
				fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			}
		} catch (FileNotFoundException e) {
			logger.warn(url + " no longer exist " + e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	Map<URL, File> createUrlToFile(Collection<ExternalFile> files) {
		Map<URL, File> urlToFile = new HashMap<>();
		for (ExternalFile file : files) {
			putUrlToFile(urlToFile, file.originalUrl());
			putUrlToFile(urlToFile, file.previewUrl());
		}
		return urlToFile;
	}

	void putUrlToFile(Map<URL, File> urlToFile, String urlString) {
		Optional<File> file = createFile(urlString);
		if (file.isPresent()) {
			try {
				URL url = new URI(urlString).toURL();
				urlToFile.put(url, file.get());
			} catch (MalformedURLException | URISyntaxException e) {
				logger.error(e.getMessage() + " error happen while parsing URL " + urlString);
			}
		}
	}

	Optional<File> createFile(String url) {
		try {
			int startOfDirName = url.indexOf("://");
			if (startOfDirName > -1) {
				// exclude protocol separator from the name
				startOfDirName = +3;
				int endOfDirName = url.lastIndexOf("/") + 1;
				int endOfFileName = url.lastIndexOf("?");
				String subDirPath = endOfFileName > startOfDirName ? url.substring(startOfDirName, endOfDirName) : url.substring(startOfDirName);
				String dirPath = filesDirPath + separator + subDirPath;
				File dir = new File(dirPath);
				if (dir.exists() && dir.isDirectory() || dir.mkdirs()) {
					String filePath = endOfFileName > -1 ? getShorter(url.substring(endOfDirName, endOfFileName), FILE) : getShorter(url.substring(endOfDirName), FILE);
					return Optional.of(new File(dir, filePath));
				} else {
					logger.error("failed to create dir {}", dir);
				}
			}
		} catch (RuntimeException e) {
			logger.error(e.getMessage() + " while handling url=", e);
		}
		return Optional.empty();
	}

	private String getShorter(String fileName, FileSystemLimit limit) {
		return fileName.length() <= limit.length ? fileName : fileName.substring(fileName.length() - limit.length);
	}

}

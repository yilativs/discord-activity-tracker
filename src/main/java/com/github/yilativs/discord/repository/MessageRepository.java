package com.github.yilativs.discord.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.yilativs.discord.model.Message;

public class MessageRepository {

	private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

	private static final String INSERT_MESSAGE_SQL = """
			INSERT INTO discord.message
			(userName, gmtDateTime, avatarUrl, messageText, repliedText, videoOriginalUrl, videoPreviewUrl, imageOriginalUrl, imagePreviewUrl, emojeTexts)
			VALUES
			(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT (userName,gmtDateTime) DO NOTHING;
			""";
	private String url;
	private String user;
	private String password;

	public MessageRepository(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;
		// TODO add flyway/liquibase support here
	}

	public void save(Collection<Message> messages) {
		try (Connection connection = DriverManager.getConnection(url, user, password)) {
			PreparedStatement ps = connection.prepareStatement(INSERT_MESSAGE_SQL);
			for (Message message : messages) {
				ps.setString(1, message.username());
				ps.setObject(2, message.gmtDateTime());
				ps.setString(3, message.avatarUrl());
				ps.setString(4, message.text());
				ps.setString(5, message.repliedText());
				ps.setString(6, message.video() == null ? null : message.video().originalUrl());
				ps.setString(7, message.video() == null ? null : message.video().previewUrl());
				ps.setString(8, message.image() == null ? null : message.image().originalUrl());
				ps.setString(9, message.image() == null ? null : message.image().previewUrl());
				ps.setObject(10, message.emojes().stream().map(e -> e.emojiText()).toList().toArray(new String[0]));
				ps.addBatch();
				ps.clearParameters();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			logger.error("storing messages to db failed because of " + e.getMessage());
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}

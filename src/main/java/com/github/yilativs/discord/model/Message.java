package com.github.yilativs.discord.model;

import java.time.LocalDateTime;
import java.util.List;

//<li id="chat-messages-693208207895822366-1143869204500582470" class="messageListItem__6a4fb"

//TODO Image support, add emoj support, add image download
public record Message(
		//<span class="username_d30d99 desaturateUserColors_b72bd3 clickable_d866f1" aria-expanded="false" role="button" tabindex="0">Vitaliy</span>
		String username,
		
		//<time aria-label="October 18, 2023 12:32 PM" id="message-timestamp-1164148929085067324" datetime="2023-10-18T10:32:13.535Z"
		LocalDateTime gmtDateTime,
		
		// <div class=contents_f41bb2
		// img avatar__08316 clickable_d866f1 src=avatarUrl
		String avatarUrl,

		String repliedText,
		
		ExternalFile video,

		ExternalFile image,

		List<Emoji> emojes,

// <div id="message-text-1143869204500582470" class="markup_a7e664
// messageContent__21e69"> divContet
		String text) {

}

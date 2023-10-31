package com.github.yilativs.discord.model;

import java.util.List;

//<li id="chat-messages-693208207895822366-1143869204500582470" class="messageListItem__6a4fb"

//TODO Image support, add emoj support, add image download
public record Message(
// from the li id
		long unixTimestamp,
		// <div class=contents_f41bb2
		// img avatar__08316 clickable_d866f1 src=avatarUrl
		String avatarUrl,

		String repliedText,

		Image image,

		List<Emoji> emojes,

// <div id="message-text-1143869204500582470" class="markup_a7e664
// messageContent__21e69"> divContet
		String text) {

}

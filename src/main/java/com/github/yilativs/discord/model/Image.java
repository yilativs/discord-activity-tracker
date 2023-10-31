package com.github.yilativs.discord.model;

public record Image(
		// <div.class=imageWrapper_fd6587-><a.data=role='img' herf=bigImage
		// data-safe-src=smallImage
		String originalUrl,
		String previewUrl) {

}

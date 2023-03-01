package com.bigboxer23.meural_control.data;

import lombok.Data;

/** */
@Data
public class OpenAIMessage {
	private String role;

	private String content;

	public OpenAIMessage(String role, String content) {
		setRole(role);
		setContent(content);
	}
}

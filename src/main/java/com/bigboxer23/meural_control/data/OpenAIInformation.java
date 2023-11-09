package com.bigboxer23.meural_control.data;

import lombok.Data;

/** */
@Data
public class OpenAIInformation {
	private String prompt;

	private String quality;

	private String style;

	public OpenAIInformation(String prompt, String quality, String style) {
		setPrompt(prompt);
		setQuality(quality);
		setStyle(style);
	}
}

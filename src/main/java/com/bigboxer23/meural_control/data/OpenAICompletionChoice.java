package com.bigboxer23.meural_control.data;

import lombok.Data;

/** */
@Data
public class OpenAICompletionChoice {
	private String text;

	private OpenAIMessage message;

	private int index;
	private String finish_reason;
}

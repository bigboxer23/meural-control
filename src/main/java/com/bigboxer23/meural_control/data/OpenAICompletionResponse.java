package com.bigboxer23.meural_control.data;

import lombok.Data;

/**
 *
 */
@Data
public class OpenAICompletionResponse
{
	private String id;
	private int created;
	private OpenAICompletionChoice[] choices;
}

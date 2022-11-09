package com.bigboxer23.meural_control.data;

import lombok.Data;

import java.util.UUID;

/**
 *
 */
@Data
public class OpenAIImageGenerationBody extends AbstractOpenAIBody
{
	private String prompt;

	private int n = 1;

	private String size = "1024x1024";

	public OpenAIImageGenerationBody(String prompt, String user)
	{
		super(user);
		this.prompt = prompt;
	}
}

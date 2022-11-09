package com.bigboxer23.meural_control.data;

import lombok.Data;

/**
 *
 */
@Data
public class OpenAICompletionBody extends AbstractOpenAIBody
{
	private String model = "text-davinci-002";
	private String prompt;
	private int max_tokens = 25;
	private double temperature = 0.99;

	public OpenAICompletionBody(String prompt, String user)
	{
		super(user);
		this.prompt = prompt;
	}
}

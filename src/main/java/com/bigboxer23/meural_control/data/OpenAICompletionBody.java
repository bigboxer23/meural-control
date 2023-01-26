package com.bigboxer23.meural_control.data;

import lombok.Data;

/** */
@Data
public class OpenAICompletionBody extends AbstractOpenAIBody {
	private String model = "text-davinci-003";
	private String prompt;
	private int max_tokens = 45;
	private double temperature = 0.99;

	private double frequency_penalty = 2;

	private double presence_penalty = 2;

	// private int n = 3;

	public OpenAICompletionBody(String prompt, String user) {
		super(user);
		this.prompt = prompt;
	}
}

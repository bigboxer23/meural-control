package com.bigboxer23.meural_control.data;

import lombok.Data;

import java.util.UUID;

/**
 *
 */
@Data
public class OpenAIImageGenerationBody
{
	private String prompt;

	private int n = 1;

	private String size = "1024x1024";

	private String user;

	public OpenAIImageGenerationBody(String prompt, String user)
	{
		this.prompt = prompt;
		//use our meural account to generate a unique id for OpenAI (https://beta.openai.com/docs/usage-policies/end-user-ids)
		this.user = UUID.nameUUIDFromBytes(user.getBytes()).toString();
	}
}

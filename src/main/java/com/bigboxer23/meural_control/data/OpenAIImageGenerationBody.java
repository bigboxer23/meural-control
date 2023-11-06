package com.bigboxer23.meural_control.data;

import lombok.Data;

/** */
@Data
public class OpenAIImageGenerationBody extends AbstractOpenAIBody {
	private String prompt;

	private int n = 1;

	private String size = "1024x1792";

	private String model = "dall-e-3";

	private String quality = "hd";

	private String style = "vivid";

	public OpenAIImageGenerationBody(String prompt, String user) {
		super(user);
		this.prompt = prompt;
	}
}

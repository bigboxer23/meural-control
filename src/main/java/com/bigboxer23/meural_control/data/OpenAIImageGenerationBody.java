package com.bigboxer23.meural_control.data;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/** */
@Data
public class OpenAIImageGenerationBody extends AbstractOpenAIBody {
	private String prompt;

	private int n = 1;

	private String size = "1024x1792";

	private String model = "dall-e-3";

	private String quality;

	private String style;

	public OpenAIImageGenerationBody(String prompt, String user, String style, String quality) {
		super(user);
		setPrompt(prompt);
		setStyle(StringUtils.defaultIfBlank(style, "vivid"));
		setQuality(StringUtils.defaultIfBlank(quality, "hd"));
	}
}

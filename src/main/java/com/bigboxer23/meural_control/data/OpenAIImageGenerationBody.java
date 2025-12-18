package com.bigboxer23.meural_control.data;

import com.squareup.moshi.Json;
import lombok.Data;

/** */
@Data
public class OpenAIImageGenerationBody extends AbstractOpenAIBody {
	private String prompt;

	private int n = 1;

	private String size = "1024x1536";

	private String model = "gpt-image-1.5";

	private String quality;

	@Json(name = "output_format")
	private String outputFormat;

	private String moderation;

	public OpenAIImageGenerationBody(String prompt, String user, String style, String quality) {
		super(user);
		setPrompt(prompt);
		setOutputFormat("jpeg");
		setModeration("low");
		setQuality("high");
	}
}

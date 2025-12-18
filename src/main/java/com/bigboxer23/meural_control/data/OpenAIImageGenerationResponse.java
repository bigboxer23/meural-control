package com.bigboxer23.meural_control.data;

import com.squareup.moshi.Json;
import lombok.Data;

/** */
@Data
public class OpenAIImageGenerationResponse {
	private long created;

	private String background;

	private OpenAIGeneratedImage[] data;

	@Json(name = "output_format")
	private String outputFormat;

	private String quality;

	private String size;

	private Usage usage;

	@Data
	public static class Usage {
		@Json(name = "input_tokens")
		private int inputTokens;

		@Json(name = "input_tokens_details")
		private InputTokensDetails inputTokensDetails;

		@Json(name = "output_tokens")
		private int outputTokens;

		@Json(name = "total_tokens")
		private int totalTokens;
	}

	@Data
	public static class InputTokensDetails {
		@Json(name = "image_tokens")
		private int imageTokens;

		@Json(name = "text_tokens")
		private int textTokens;
	}
}

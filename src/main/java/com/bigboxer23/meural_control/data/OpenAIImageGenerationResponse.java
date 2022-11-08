package com.bigboxer23.meural_control.data;

import lombok.Data;

/**
 *
 */
@Data
public class OpenAIImageGenerationResponse
{
	private int created;

	private OpenAIGeneratedImage[] data;
}

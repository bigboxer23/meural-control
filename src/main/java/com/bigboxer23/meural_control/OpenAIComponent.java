package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.*;
import com.squareup.moshi.Moshi;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * component that calls OpenAI's image generation API to generate an image from the text prompt
 */
@Component
public class OpenAIComponent implements IMeuralImageSource
{
	private static final Logger logger = LoggerFactory.getLogger(OpenAIComponent.class);

	private static final MediaType JSON
			= MediaType.parse("application/json; charset=utf-8");
	@Value("${openai-key}")
	private String apiKey;

	@Value("${openai-prompt}")
	private String prompt;

	@Value("${meural-account}")
	private String user;

	@Value("${openai-save-album}")
	private String albumToSaveTo;

	private final OkHttpClient client = new OkHttpClient();

	private final Moshi moshi = new Moshi.Builder().build();

	@Override
	public Optional<SourceItem> nextItem()
	{
		return generateItem();
	}

	@Override
	public Optional<SourceItem> prevItem()
	{
		return generateItem();
	}

	public void updatePrompt(String newPrompt)
	{
		prompt = newPrompt;
	}

	private Optional<SourceItem> generateItem()
	{
		logger.info("Requesting generated image for prompt: \"" + prompt + "\"");
		RequestBody body = RequestBody.create(moshi.adapter(OpenAIImageGenerationBody.class)
				.toJson(new OpenAIImageGenerationBody(prompt, user)), JSON);
		Request request = new Request
				.Builder()
				.url("https://api.openai.com/v1/images/generations")
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + apiKey)
				.post(body)
				.build();
		try (Response response = client.newCall(request).execute())
		{
			if (response.isSuccessful())
			{
				OpenAIImageGenerationResponse openAIResponse = moshi
						.adapter(OpenAIImageGenerationResponse.class)
						.fromJson(response.body().string());
				if (openAIResponse.getData().length > 0)
				{
					return Optional.of(new SourceItem(prompt + ".png", new URL(openAIResponse.getData()[0].getUrl()), albumToSaveTo));
				}
			}
		} catch (IOException e)
		{
			logger.warn("generateItem", e);
		}
		return Optional.empty();
	}
}

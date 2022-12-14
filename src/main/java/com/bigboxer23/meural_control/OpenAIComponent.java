package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.*;
import com.bigboxer23.meural_control.google.GoogleCalendarComponent;
import com.bigboxer23.utils.FilePersistentIndex;
import com.squareup.moshi.Moshi;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
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

	private String prompt;

	@Value("${meural-account}")
	private String user;

	@Value("${openai-save-album}")
	private String albumToSaveTo;

	private File lastPrompt = new File(System.getProperty("user.dir") + File.separator + FilePersistentIndex.kPrefix + "openAIPrompt");

	private final OkHttpClient client = new OkHttpClient();

	private final Moshi moshi = new Moshi.Builder().build();

	private GoogleCalendarComponent gCalendarComponent;

	private Environment env;

	public OpenAIComponent(Environment env, GoogleCalendarComponent gCalendarComp)
	{
		this.env = env;
		prompt = env.getProperty("openai-prompt");//Do here instead of via annotation, so we can control ordering
		if (lastPrompt.exists())
		{
			try
			{
				prompt = FileUtils.readFileToString(lastPrompt, Charset.defaultCharset());
			} catch (IOException e)
			{
				logger.warn("error reading prompt", e);
			}
		}
		gCalendarComponent = gCalendarComp;
	}

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
		prompt = newPrompt.trim().replace("\n", " ");
		try
		{
			FileUtils.writeStringToFile(lastPrompt, prompt, Charset.defaultCharset(), false);
		} catch (IOException e)
		{
			logger.warn("error writing prompt", e);
		}
	}

	private Optional<SourceItem> generateItem()
	{
		generateNewPrompt(true).ifPresent(this::updatePrompt);
		logger.info("Requesting generated image for prompt: \"" + prompt + gCalendarComponent.getHolidayString() + "\"");
		RequestBody body = RequestBody.create(moshi.adapter(OpenAIImageGenerationBody.class)
				.toJson(new OpenAIImageGenerationBody(prompt + gCalendarComponent.getHolidayString(), user)), JSON);
		try (Response response = client.newCall(getRequest("v1/images/generations", body)).execute())
		{
			if (response.isSuccessful())
			{
				OpenAIImageGenerationResponse openAIResponse = moshi
						.adapter(OpenAIImageGenerationResponse.class)
						.fromJson(response.body().string());
				if (openAIResponse.getData().length > 0)
				{
					return Optional.of(new SourceItem(prompt + gCalendarComponent.getHolidayString() + ".png", new URL(openAIResponse.getData()[0].getUrl()), albumToSaveTo));
				}
			} else
			{
				logger.warn("request was not successful for " + prompt + gCalendarComponent.getHolidayString() + ". "
						+ response.body().string() + " " + response.code());
				prompt = env.getProperty("openai-prompt");
			}
		} catch (IOException e)
		{
			logger.warn("generateItem", e);
		}
		return Optional.empty();
	}

	private Optional<String> generateNewPrompt(boolean shouldRetry)
	{
		logger.info("Requesting generated prompt: \"" + prompt + "\"");
		RequestBody body = RequestBody.create(moshi.adapter(OpenAICompletionBody.class)
				.toJson(new OpenAICompletionBody("generate a random art prompt based on: " + prompt, user)), JSON);
		try (Response response = client.newCall(getRequest("v1/completions", body)).execute())
		{
			if (response.isSuccessful())
			{
				OpenAICompletionResponse openAIResponse = moshi
						.adapter(OpenAICompletionResponse.class)
						.fromJson(response.body().string());
				if (openAIResponse != null
						&& openAIResponse.getChoices().length > 0
						&& openAIResponse.getChoices()[0].getText().length() > 0)
				{
					String text = openAIResponse.getChoices()[0].getText().trim();
					if (prompt.equals(text) && text.split(" ").length < 6)
					{
						throw new IOException(text + " is not complex enough, trying again");
					}
					logger.info("new prompt generated: \"" + text + "\"");
					return Optional.of(text);
				}
			}
		} catch (IOException e)
		{
			logger.warn("generateNewPrompt", e);
		}
		if (shouldRetry)
		{
			return generateNewPrompt(false);//if we fail to get a suggestion, try once more
		}
		logger.warn("generated empty suggestion");
		return Optional.empty();
	}

	private Request getRequest(String url, RequestBody body)
	{
		return new Request
				.Builder()
				.url("https://api.openai.com/" + url)
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + apiKey)
				.post(body)
				.build();
	}
}

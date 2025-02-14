package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.*;
import com.bigboxer23.meural_control.google.GoogleCalendarComponent;
import com.bigboxer23.utils.file.FilePersistedString;
import com.bigboxer23.utils.http.OkHttpUtil;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** component that calls OpenAI's image generation API to generate an image from the text prompt */
@Slf4j
@Component
public class OpenAIComponent implements IMeuralImageSource {
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	@Value("${openai-key}")
	private String apiKey;

	@Value("${meural-account}")
	private String user;

	@Value("${openai-save-album}")
	private String albumToSaveTo;

	private final FilePersistedString lastPrompt = new FilePersistedString("openAIPrompt");

	private final Moshi moshi = new Moshi.Builder().build();

	private final GoogleCalendarComponent gCalendarComponent;

	private final Environment env;

	private int mode;

	private final FilePersistedString style = new FilePersistedString("dalleStyle");

	private final FilePersistedString quality = new FilePersistedString("dalleQuality");

	public OpenAIComponent(Environment env, GoogleCalendarComponent gCalendarComp) {
		this.env = env;
		if (lastPrompt.get().isBlank()) {
			lastPrompt.set(env.getProperty("openai-prompt")); // Do here instead of via annotation, so we can
			// control
		}
		gCalendarComponent = gCalendarComp;
	}

	@Override
	public Optional<SourceItem> nextItem() {
		return generateItem();
	}

	@Override
	public Optional<SourceItem> prevItem() {
		return generateItem();
	}

	public void updatePrompt(String newPrompt) {
		lastPrompt.set(StringUtils.truncate(newPrompt.trim().replace("\n", " "), 900));
	}

	protected Optional<SourceItem> generateItem() {
		if (mode == 1) {
			generateNewPromptTextCompletion(true).ifPresent(this::updatePrompt);
		} else if (mode == 2 || mode == 3) {
			generateNewPrompt().ifPresent(this::updatePrompt);
		}
		log.info("Requesting generated image for prompt: \""
				+ lastPrompt.get()
				+ gCalendarComponent.getHolidayString()
				+ "\"");
		RequestBody body = RequestBody.create(
				moshi.adapter(OpenAIImageGenerationBody.class)
						.toJson(new OpenAIImageGenerationBody(
								lastPrompt.get() + gCalendarComponent.getHolidayString(),
								user,
								style.get(),
								quality.get())),
				JSON);
		try (Response response = getRequest("v1/images/generations", body)) {
			if (response.isSuccessful()) {
				OpenAIImageGenerationResponse openAIResponse =
						OkHttpUtil.getNonEmptyBody(response, OpenAIImageGenerationResponse.class);
				if (openAIResponse.getData().length > 0) {
					return Optional.of(new SourceItem(
							lastPrompt.get() + gCalendarComponent.getHolidayString() + ".png",
							new URL(openAIResponse.getData()[0].getUrl()),
							albumToSaveTo));
				}
			} else {
				resetPrompt(response.message(), response.code());
			}
		} catch (IOException e) {
			log.warn("generateItem", e);
		}
		return Optional.empty();
	}

	private void resetPrompt(String body, int code) {
		log.warn("request was not successful for "
				+ lastPrompt.get()
				+ gCalendarComponent.getHolidayString()
				+ ". "
				+ body
				+ " "
				+ code);
		lastPrompt.set(env.getProperty("openai-prompt"));
	}

	protected Optional<String> generateNewPrompt() {
		log.info("Requesting generated prompt: \"" + lastPrompt.get() + "\"");
		RequestBody body = RequestBody.create(
				moshi.adapter(OpenAIChatCompletionBody.class)
						.toJson(new OpenAIChatCompletionBody(
								"generate a random art prompt based on: " + lastPrompt.get(), user, mode)),
				JSON);
		try (Response response = getRequest("v1/chat/completions", body)) {
			if (response.isSuccessful()) {
				OpenAICompletionResponse openAIResponse =
						OkHttpUtil.getNonEmptyBody(response, OpenAICompletionResponse.class);
				if (openAIResponse != null && openAIResponse.getChoices().length > 0) {
					String text = openAIResponse
							.getChoices()[0]
							.getMessage()
							.getContent()
							.trim();
					log.info("new prompt generated: \"" + text + "\"");
					return Optional.of(text);
				}
			}
		} catch (IOException e) {
			log.warn("generateNewPrompt", e);
		}
		log.warn("generated empty suggestion");
		return Optional.empty();
	}

	private Optional<String> generateNewPromptTextCompletion(boolean shouldRetry) {
		log.info("Requesting generated prompt: \"" + lastPrompt.get() + "\"");
		RequestBody body = RequestBody.create(
				moshi.adapter(OpenAICompletionBody.class)
						.toJson(new OpenAICompletionBody(
								"generate a random art prompt based on: " + lastPrompt.get(), user)),
				JSON);
		try (Response response = getRequest("v1/completions", body)) {
			if (response.isSuccessful()) {
				OpenAICompletionResponse openAIResponse =
						OkHttpUtil.getNonEmptyBody(response, OpenAICompletionResponse.class);
				if (openAIResponse != null && openAIResponse.getChoices().length > 0) {
					String text = openAIResponse.getChoices()[0].getText().trim();
					if (lastPrompt.get().equals(text) || text.split(" ").length < 6) {
						if (!shouldRetry && !lastPrompt.get().equalsIgnoreCase(env.getProperty("openai-prompt"))) {
							resetPrompt(response.message(), response.code());
							shouldRetry = true;
						}
						throw new IOException(text + " is not complex enough, trying again");
					}
					log.info("new prompt generated: \"" + text + "\"");
					return Optional.of(text);
				}
			}
		} catch (IOException e) {
			log.warn("generateNewPrompt", e);
		}
		if (shouldRetry) {
			return generateNewPromptTextCompletion(false); // if we fail to get a suggestion, try once more
		}
		log.warn("generated empty suggestion");
		return Optional.empty();
	}

	private Response getRequest(String url, RequestBody body) throws IOException {
		return OkHttpUtil.postSynchronous(
				"https://api.openai.com/" + url, body, builder -> builder.header("Content-Type", "application/json")
						.header("Authorization", "Bearer " + apiKey));
	}

	public Optional<String> getPrompt() {
		return Optional.ofNullable(lastPrompt.get());
	}

	public void setMode(int theMode) {
		mode = theMode;
	}

	public void setStyle(String style) {
		log.info("setting style to " + style);
		this.style.set(style);
	}

	public void setQuality(String quality) {
		log.info("setting quality to " + quality);
		this.quality.set(quality);
	}

	public String getQuality() {
		return quality.get();
	}

	public String getStyle() {
		return style.get();
	}
}

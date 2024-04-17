package com.bigboxer23.meural_control;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.meural_control.data.OpenAIImageGenerationBody;
import com.bigboxer23.meural_control.data.OpenAIImageGenerationResponse;
import com.bigboxer23.meural_control.data.SourceItem;
import com.bigboxer23.utils.http.OkHttpUtil;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.util.Optional;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OpenAIComponentTest {
	@Autowired
	private OpenAIComponent component;

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	public void testGenerateItem(int mode) {
		component.setMode(mode);
		assertTrue(component.getPrompt().isPresent());
		String prompt = component.getPrompt().get();
		Optional<SourceItem> item = component.generateItem();
		assertTrue(item.isPresent());
		assertNotNull(item.get().getUrl());
		assertNotEquals(prompt, component.getPrompt().get());
	}

	@Value("${openai-key}")
	private String apiKey;

	@Value("${meural-account}")
	private String user;

	private final Moshi moshi = new Moshi.Builder().build();

	@Test
	public void tmp() {
		RequestBody body = RequestBody.create(
				moshi.adapter(OpenAIImageGenerationBody.class)
						.toJson(new OpenAIImageGenerationBody(
								"generate an image of barbies dressed like the" + " village people",
								user,
								"vivid",
								"hd")),
				MediaType.parse("application/json; charset=utf-8"));
		try (Response response = OkHttpUtil.postSynchronous(
				"https://api.openai.com/v1/images/generations", body, builder -> builder.header(
								"Content-Type", "application/json")
						.header("Authorization", "Bearer " + apiKey))) {
			if (response.isSuccessful()) {
				OpenAIImageGenerationResponse openAIResponse = moshi.adapter(OpenAIImageGenerationResponse.class)
						.fromJson(response.body().string());
				if (openAIResponse.getData().length > 0) {
					System.out.println(openAIResponse.getData()[0].getUrl());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			// logger.warn("generateItem", e);
		}
	}
}

package com.bigboxer23.meural_control.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** */
@Data
public class OpenAIChatCompletionBody extends AbstractOpenAIBody {
	private String model = "gpt-3.5-turbo";

	private List<OpenAIMessage> messages = new ArrayList<>();

	public OpenAIChatCompletionBody(String prompt, String user, int mode) {
		super(user);
		messages.add(new OpenAIMessage("system", "you are a helpful assistant that generates works of art"));
		messages.add(new OpenAIMessage("user", prompt));
		if (mode == 3) {
			model = "gpt-4";
		}
	}
}

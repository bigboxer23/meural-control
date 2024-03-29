package com.bigboxer23.meural_control.data;

import java.util.UUID;
import lombok.Data;

/** */
@Data
public class AbstractOpenAIBody {
	private String user;

	public AbstractOpenAIBody() {}

	public AbstractOpenAIBody(String user) {
		// use our meural account to generate a unique id for OpenAI
		// (https://beta.openai.com/docs/usage-policies/end-user-ids)
		this.user = UUID.nameUUIDFromBytes(user.getBytes()).toString();
	}
}

package com.bigboxer23.meural_control.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** */
@Data
public class MeuralStringResponse extends MeuralResponse {
	@Schema(
			description = "Gives more specific reason request is successful or fails",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "Unsupported file format uploaded")
	private String response;
}

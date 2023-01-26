package com.bigboxer23.meural_control.data;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MeuralResponse {
	@Schema(
			description = "Was the request a success or failure",
			required = true,
			allowableValues = {"pass", "fail"})
	private String status = "fail";

	@Hidden
	public boolean isSuccessful() {
		return "pass".equals(getStatus());
	}
}

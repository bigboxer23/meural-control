package com.bigboxer23.meural_control.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** Meural response JSON for returning boolean response */
@Data
public class MeuralStatusResponse extends MeuralResponse {
	@Schema(description = "Give status data, true or false", required = true)
	private boolean response;
}

package com.bigboxer23.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Meural response JSON for returning boolean response
 */
@Data
public class MeuralStatusResponse
{
	@Schema(description = "Was the request a success or failure", required = true, allowableValues = {"pass", "fail"})
	private String status;

	@Schema(description = "Give status data, true or false", required = true)
	private boolean response;
}

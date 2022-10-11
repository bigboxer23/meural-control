package com.bigboxer23.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MeuralResponse
{
	@Schema(description = "Was the request a success or failure", required = true, allowableValues = {"pass", "fail"})
	private String status;

	@Schema(description = "Gives more specific reason request is successful or fails", required = true, example = "Unsupported file format uploaded")
	private String response;
}

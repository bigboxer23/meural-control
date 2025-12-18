package com.bigboxer23.meural_control.data;

import com.squareup.moshi.Json;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import lombok.Data;

/** */
@Data
public class OpenAIGeneratedImage {
	@Json(name = "b64_json")
	private String b64Json;

	/**
	 * Converts the base64-encoded image data to a file
	 *
	 * @param outputPath the path where the file should be written
	 * @return the created File object
	 * @throws IOException if there's an error writing the file
	 */
	public File toFile(String outputPath) throws IOException {
		if (b64Json == null || b64Json.isEmpty()) {
			throw new IllegalStateException("No base64 JSON data available");
		}

		byte[] imageBytes = Base64.getDecoder().decode(b64Json);
		File outputFile = new File(outputPath);

		try (FileOutputStream fos = new FileOutputStream(outputFile)) {
			fos.write(imageBytes);
		}

		return outputFile;
	}
}

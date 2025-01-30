package com.bigboxer23.meural_control;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component to apply command against an image prior to display. Useful for things like cropping or
 * adjusting image in some way via imageMagick or similar
 */
@Slf4j
@Component
public class ImageTransformComponent {
	@Value("${transform-preview-commmand}")
	private String previewCommand;

	@Value("${transform-commmand}")
	private String command;

	public File transformPreviewItem(File file) {
		return transformItem(file, previewCommand);
	}

	public File transformItem(File file) {
		return transformItem(file, command);
	}

	private File transformItem(File file, String command) {
		if (command == null || command.equals("")) {
			return file;
		}
		File tmpFile = new File(System.getProperty("user.dir") + File.separator + "transformed.jpg");
		if (tmpFile.exists()) {
			tmpFile.delete();
		}
		command = String.format(command, file.getAbsolutePath(), tmpFile.getAbsolutePath());
		ProcessBuilder builder = new ProcessBuilder(command.split(" "));
		try {
			Process process = builder.start();
			StringBuilder out = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					out.append(line).append("\n");
				}
				log.info("command output: " + out);
			}
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new IOException("error exit status from command: " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			log.warn("error running command", e);
		}
		return tmpFile.exists() ? tmpFile : file;
	}
}

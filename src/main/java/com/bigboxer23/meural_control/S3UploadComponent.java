package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.SourceItem;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/** Component to upload images to S3 bucket */
@Slf4j
@Component
public class S3UploadComponent {
	@Value("${s3.bucket.name}")
	private String bucketName;

	@Value("${s3.region:us-east-1}")
	private String region;

	private S3Client s3Client;

	private S3Client getS3Client() {
		if (s3Client == null) {
			s3Client = S3Client.builder()
					.region(Region.of(region))
					.credentialsProvider(DefaultCredentialsProvider.builder().build())
					.build();
		}
		return s3Client;
	}

	/**
	 * Upload an item to the configured S3 bucket
	 *
	 * @param item The source item to upload
	 * @return The S3 key of the uploaded file, or null if upload failed
	 */
	public String uploadItem(SourceItem item) {
		if (item == null || item.getTempFile() == null || !item.getTempFile().exists()) {
			log.warn("uploadItem: invalid item or missing temp file");
			return null;
		}

		try {
			String key = generateS3Key(item);
			String contentType = getContentType(item.getTempFile().getName());

			log.info("uploading item to S3 bucket: \"{}\" with key: \"{}\"", bucketName, key);

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.contentType(contentType)
					.build();

			PutObjectResponse response =
					getS3Client().putObject(putObjectRequest, RequestBody.fromFile(item.getTempFile()));

			if (response != null && response.sdkHttpResponse().isSuccessful()) {
				log.info("Successfully uploaded item to S3: {}", key);
				return key;
			} else {
				log.warn("Failed to upload item to S3");
				return null;
			}
		} catch (Exception e) {
			log.error("uploadItem error", e);
			return null;
		}
	}

	/**
	 * Generate S3 key for the uploaded file. Uses a date folder (YYYY-MM), timestamp and original
	 * filename to ensure uniqueness while maintaining readability.
	 */
	private String generateS3Key(SourceItem item) {
		String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
		String timestamp = String.valueOf(System.currentTimeMillis());
		String extension = FilenameUtils.getExtension(item.getTempFile().getName());
		String baseName = FilenameUtils.getBaseName(item.getName());

		// Clean the base name to be S3-friendly (remove special characters)
		baseName = baseName.replaceAll("[^a-zA-Z0-9-_]", "_");

		return String.format("%s/%s/%s_%s.%s", "meural-uploads", dateFolder, timestamp, baseName, extension);
	}

	/** Determine content type based on file extension */
	private String getContentType(String filename) {
		String extension = FilenameUtils.getExtension(filename).toLowerCase();
		return switch (extension) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			case "gif" -> "image/gif";
			default -> "application/octet-stream";
		};
	}
}

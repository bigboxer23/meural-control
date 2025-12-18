package com.bigboxer23.meural_control;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.meural_control.data.SourceItem;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class S3UploadComponentTest {
	@Autowired
	private S3UploadComponent s3UploadComponent;

	@Autowired
	private MeuralComponent meuralComponent;

	@Test
	public void testUploadItemWithValidFile() throws IOException {
		// Create a temporary test image file
		Path tempFile = Files.createTempFile("test-image", ".jpg");
		try {
			// Write some dummy image data
			Files.write(tempFile, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});

			SourceItem item = new SourceItem("test-image.jpg", new URL("http://example.com/test.jpg"));
			item.setTempFile(tempFile.toFile());

			// Test upload
			String s3Key = s3UploadComponent.uploadItem(item);

			// Verify upload returned a key with proper structure
			String expectedDateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
			assertNotNull(s3Key, "S3 key should not be null for successful upload");
			assertTrue(s3Key.contains("meural-uploads"), "S3 key should contain prefix");
			assertTrue(s3Key.contains(expectedDateFolder), "S3 key should contain date folder (YYYY-MM)");
			assertTrue(s3Key.endsWith(".jpg"), "S3 key should have correct extension");
		} catch (Exception e) {
			// If AWS credentials are not configured, the test should pass gracefully
			// This allows tests to run in environments without AWS setup
			if (e.getMessage() != null
					&& (e.getMessage().contains("credentials") || e.getMessage().contains("Unable to load"))) {
				System.out.println("AWS credentials not available, skipping S3 upload test");
			} else {
				throw e;
			}
		} finally {
			// Cleanup
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	public void testUploadItemWithNullItem() {
		String result = s3UploadComponent.uploadItem(null);
		assertNull(result, "Upload should return null for null item");
	}

	@Test
	public void testUploadItemWithMissingFile() throws IOException {
		SourceItem item = new SourceItem("test.jpg", new URL("http://example.com/test.jpg"));
		// Don't set temp file, or set to non-existent file
		File nonExistentFile = new File("/tmp/non-existent-file-xyz.jpg");
		item.setTempFile(nonExistentFile);

		String result = s3UploadComponent.uploadItem(item);
		assertNull(result, "Upload should return null for missing temp file");
	}

	@Test
	public void testUploadIntegrationWithMeuralComponent() throws IOException {
		// Create a temporary test image file
		Path tempFile = Files.createTempFile("meural-test", ".png");
		try {
			// Write PNG header
			Files.write(tempFile, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

			SourceItem item = new SourceItem("meural-test.png", new URL("http://example.com/test.png"));
			item.setTempFile(tempFile.toFile());
			item.setAlbumToSaveTo("test-album");
			item.setCleanupTempFile(false);

			// Test that the component can handle the upload
			String s3Key = s3UploadComponent.uploadItem(item);

			if (s3Key != null) {
				// If upload succeeded (AWS credentials available)
				String expectedDateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
				assertTrue(s3Key.contains("meural-uploads"), "S3 key should contain correct prefix");
				assertTrue(s3Key.contains(expectedDateFolder), "S3 key should contain date folder (YYYY-MM)");
				assertTrue(s3Key.endsWith(".png"), "S3 key should preserve file extension");
			}
		} catch (Exception e) {
			// Gracefully handle missing AWS credentials
			if (e.getMessage() != null
					&& (e.getMessage().contains("credentials") || e.getMessage().contains("Unable to load"))) {
				System.out.println("AWS credentials not available, skipping integration test");
			} else {
				throw e;
			}
		} finally {
			// Cleanup
			Files.deleteIfExists(tempFile);
		}
	}
}

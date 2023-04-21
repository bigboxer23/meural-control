package com.bigboxer23.meural_control;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.meural_control.data.SourceItem;
import com.bigboxer23.meural_control.google.GooglePhotosComponent;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ImageTransformComponentTest {
	@Autowired
	private ImageTransformComponent component;

	@Autowired
	private GooglePhotosComponent photosComponent;

	@Autowired
	private MeuralComponent meuralComponent;

	@Test
	public void testTransformPreviewItem() throws IOException {
		internalTest(original -> assertNotEquals(component.transformPreviewItem(original), original));
	}

	@Test
	public void testTransformItem() throws IOException {
		internalTest(original -> assertEquals(component.transformItem(original), original));
	}

	private void internalTest(Command command) throws IOException {
		Optional<SourceItem> item = photosComponent.nextItem();
		assertTrue(item.isPresent());
		meuralComponent.fetchItem(item.get(), () -> {
			command.execute(item.get().getTempFile());
			return null;
		});
	}

	private interface Command {
		void execute(File original);
	}
}

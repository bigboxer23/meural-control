package com.bigboxer23.meural_control.google;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.meural_control.MeuralComponent;
import com.bigboxer23.meural_control.data.SourceItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.types.proto.MediaItem;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class GooglePhotosComponentTest {

	@Value("${gPhotos-albumTitle}")
	private String defaultAlbumTitle;

	@Value("${openai-save-album}")
	private String alternateAlbumTitle;

	@Autowired
	private GooglePhotosComponent component;

	@Autowired
	private MeuralComponent meuralComponent;

	@BeforeEach
	public void setup() {
		component.changeAlbum(defaultAlbumTitle);
	}

	@Test
	public void testNextItem() {
		component.setImageIndex(0);
		Optional<SourceItem> item = component.nextItem();
		assertTrue(item.isPresent());
		assertEquals(1, component.getImageIndex());
		Optional<SourceItem> item2 = component.nextItem();
		assertTrue(item2.isPresent());
		assertEquals(2, component.getImageIndex());
		assertFalse(item.get().getName().equalsIgnoreCase(item2.get().getName()));
	}

	@Test
	public void testPrevItem() {
		component.setImageIndex(5);
		Optional<SourceItem> item = component.prevItem();
		assertTrue(item.isPresent());
		assertEquals(4, component.getImageIndex());
		Optional<SourceItem> item2 = component.prevItem();
		assertTrue(item2.isPresent());
		assertEquals(3, component.getImageIndex());
		assertFalse(item.get().getName().equalsIgnoreCase(item2.get().getName()));
	}

	@Test
	public void testChangeAlbum() {
		component.setImageIndex(5);
		component.changeAlbum(alternateAlbumTitle);
		Optional<SourceItem> item = component.nextItem();
		assertTrue(item.isPresent());
		assertEquals(0, component.getImageIndex());
	}

	@Test
	public void testAlbumWrapping() {
		component.setImageIndex(500); // pick something arbitrarily high that should cause us to reset the index
		Optional<SourceItem> item = component.nextItem();
		assertTrue(item.isPresent());
		assertEquals(0, component.getImageIndex());
	}

	@Test
	public void testUpload() throws IOException {
		Optional<SourceItem> item = component.nextItem();
		assertTrue(item.isPresent());
		meuralComponent.fetchItem(
				item.get(),
				() -> { // Using this means we get cleanup of temp file for free
					SourceItem testItem = new SourceItem("TestFile" + System.currentTimeMillis(), null, "testAlbum");
					testItem.setTempFile(item.get().getTempFile());
					NewMediaItemResult result = component.uploadItemToAlbum(testItem);
					assertNotNull(result);
					MediaItem mediaItem = result.getMediaItem();
					assertNotNull(mediaItem);
					// This doesn't seem to work, the mediaid isn't matching what's there and we
					// fail
					component.removeItemFromAlbum(alternateAlbumTitle, mediaItem);
					return null;
				});
	}
}

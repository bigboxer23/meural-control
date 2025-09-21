package com.bigboxer23.meural_control.jwst;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.meural_control.data.SourceItem;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class JWSTComponentTest {

	@Autowired
	private JWSTComponent component;

	@Test
	public void testImageFetching() {
		component.setFetchedImageIndex(0);
		Optional<SourceItem> item = component.nextItem();
		Optional<SourceItem> item2 = component.nextItem();
		assertTrue(item.isPresent());
		assertTrue(item2.isPresent());
		assertNotEquals(item.get().getName(), item2.get().getName());
	}

	@Test
	public void testNextItem() {
		component.setFetchedImageIndex(0);
		assertTrue(component.nextItem().isPresent());
		assertTrue(component.getFetchedImageIndex() > 0);
	}

	@Test
	public void testPreviousItem() {
		component.setFetchedImageIndex(10);
		assertTrue(component.prevItem().isPresent());
		assertTrue(component.getFetchedImageIndex() < 10);
	}

	@Test
	public void testDiagramSkipping() {
		component.setFetchedImageIndex(0);
		for (int i = 0; i < 5; i++) {
			Optional<SourceItem> item = component.nextItem();
			assertTrue(item.isPresent());
			assertFalse(component.shouldSkipLink(item.get().getName()));
		}
	}
}

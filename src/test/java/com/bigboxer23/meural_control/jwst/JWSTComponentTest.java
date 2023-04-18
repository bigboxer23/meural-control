package com.bigboxer23.meural_control.jwst;

import static com.bigboxer23.meural_control.jwst.JWSTComponent.PAGE_SIZE;
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
	public void testPaging() {
		component.setPage(1);
		component.setFetchedImageIndex(0);
		Optional<SourceItem> item = component.nextItem();
		component.setFetchedImageIndex(PAGE_SIZE - 1);
		Optional<SourceItem> item2 = component.nextItem();
		assertTrue(item.isPresent());
		assertTrue(item2.isPresent());
		assertEquals(2, component.getPage());
		assertTrue(component.getFetchedImageIndex() < PAGE_SIZE);
		assertNotEquals(item.get().getName(), item2.get().getName());
	}

	@Test
	public void testLastPage() {
		component.setPage(35);
		component.setFetchedImageIndex(0);
		Optional<SourceItem> item = component.nextItem();
		assertTrue(item.isPresent());
		assertEquals(1, component.getPage());
	}

	@Test
	public void testNextItem() {
		component.setPage(1);
		component.setFetchedImageIndex(0);
		assertTrue(component.nextItem().isPresent());
		assertEquals(1, component.getFetchedImageIndex());
		component.setFetchedImageIndex(PAGE_SIZE - 1);
		assertTrue(component.nextItem().isPresent());
		assertEquals(0, component.getFetchedImageIndex());
		assertEquals(2, component.getPage());
	}

	@Test
	public void testPreviousItem() {
		component.setPage(1);
		component.setFetchedImageIndex(0);
		assertTrue(component.prevItem().isPresent());
		assertEquals(PAGE_SIZE - 1, component.getFetchedImageIndex());
		component.setFetchedImageIndex(0);
		component.setPage(3);
		assertTrue(component.prevItem().isPresent());
		assertEquals(PAGE_SIZE - 1, component.getFetchedImageIndex());
		assertEquals(2, component.getPage());
	}

	@Test
	public void testDiagramSkipping() {
		component.setPage(1);
		component.setFetchedImageIndex(0);
		for (int i = 0; i < PAGE_SIZE; i++) {
			Optional<SourceItem> item = component.nextItem();
			assertTrue(item.isPresent());
			assertFalse(component.shouldSkipLink(item.get().getName()));
		}
	}
}

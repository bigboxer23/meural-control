package com.bigboxer23.meural_control;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.meural_control.data.Device;
import com.bigboxer23.meural_control.data.MeuralPlaylist;
import java.io.IOException;
import java.util.Optional;

import com.bigboxer23.meural_control.data.SourceItem;
import com.bigboxer23.meural_control.google.GooglePhotosComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class MeuralComponentTest {
	@Autowired
	private MeuralComponent component;

	@Autowired
	private GooglePhotosComponent photosComponent;

	@Test
	public void testMeuralCloudAuth() {
		assertNotNull(component.getToken());
	}

	@Test
	public void testGetDeviceInfo() throws IOException {
		Device meural = component.getDevice();
		assertNotNull(meural);
		assertNotNull(meural.getId());
		assertNotNull(meural.getFrameStatus().getLocalIp());
	}

	@Test
	public void testPlaylist() throws IOException {
		MeuralPlaylist playlist = component.getOrCreatePlaylist();
		assertNotNull(playlist);
		assertNotNull(playlist.getName());
		assertNotNull(playlist.getItemIds());
	}

	@Test
	public void testPlaylistCreationDeletion() throws IOException {
		MeuralPlaylist playlist = null;
		try {
			playlist = component.createPlaylist("testPlaylist");
			assertNotNull(playlist);
			assertNotNull(playlist.getId());
		} finally {
			if (playlist != null) {
				component.deletePlaylist(playlist.getId());
			}
		}
	}

	@Test
	public void testSleep() throws IOException, InterruptedException {
		assertFalse(component.isAsleep().isResponse());
		component.sleep();
		Thread.sleep(3000);
		assertTrue(component.isAsleep().isResponse());
		component.wakeup();
		Thread.sleep(1000);
		assertFalse(component.isAsleep().isResponse());
	}

	@Test
	public void testUpDown() throws IOException {
		assertTrue(component.up().isSuccessful());
		assertTrue(component.down().isSuccessful());
	}

	@Test
	public void testPreview() throws IOException
	{
		Optional<SourceItem> item = photosComponent.nextItem();
		assertTrue(item.isPresent());
		component.previewItem(item.get(), true);
	}
}

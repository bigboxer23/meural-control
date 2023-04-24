package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.Device;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class MeuralComponentTest
{
	@Autowired
	private MeuralComponent component;

	@Test
	public void testMeuralCloudAuth() {
		assertNotNull(component.getToken());
	}

	@Test
	public void testGetDeviceInfo() throws IOException
	{
		Device meural = component.getDevice();
		assertNotNull(meural);
		assertNotNull(meural.getId());
		assertNotNull(meural.getFrameStatus().getLocalIp());
	}
}

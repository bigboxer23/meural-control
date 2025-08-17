package com.bigboxer23.meural_control;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.meural_control.data.MeuralResponse;
import com.bigboxer23.meural_control.data.MeuralStatusResponse;
import com.bigboxer23.meural_control.data.MeuralStringResponse;
import com.bigboxer23.meural_control.google.GooglePhotosComponent;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MeuralControllerTest {

	@Mock
	private MeuralComponent mockApi;

	@Mock
	private SchedulerComponent mockScheduler;

	@Mock
	private OpenAIComponent mockOpenAIComponent;

	@Mock
	private GooglePhotosComponent mockGPhotosAPI;

	@Mock
	private HttpServletResponse mockServletResponse;

	private MeuralController controller;

	@BeforeEach
	void setUp() {
		controller = new MeuralController(mockApi, mockScheduler, mockOpenAIComponent, mockGPhotosAPI);
	}

	@Test
	void testIsAsleep_WhenDeviceUnreachable_ReturnsCorrectErrorResponse() throws IOException {
		when(mockApi.isAsleep()).thenThrow(new IOException("Device unreachable"));

		MeuralStatusResponse response = controller.isAsleep(mockServletResponse);

		assertNotNull(response);
		assertInstanceOf(MeuralStatusResponse.class, response);
		assertEquals("fail", response.getStatus());
		assertFalse(response.isSuccessful());

		verify(mockApi).reset();
		verify(mockServletResponse).setStatus(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void testSleep_WhenDeviceUnreachable_ReturnsCorrectErrorResponse() throws IOException {
		when(mockApi.sleep()).thenThrow(new IOException("Device unreachable"));

		MeuralStringResponse response = controller.sleep(mockServletResponse);

		assertNotNull(response);
		assertInstanceOf(MeuralStringResponse.class, response);
		assertEquals("fail", response.getStatus());
		assertFalse(response.isSuccessful());

		verify(mockApi).reset();
		verify(mockServletResponse).setStatus(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void testWakeup_WhenDeviceUnreachable_ReturnsCorrectErrorResponse() throws IOException {
		when(mockApi.wakeup()).thenThrow(new IOException("Device unreachable"));

		MeuralStringResponse response = controller.wakeup(mockServletResponse);

		assertNotNull(response);
		assertInstanceOf(MeuralStringResponse.class, response);
		assertEquals("fail", response.getStatus());
		assertFalse(response.isSuccessful());

		verify(mockApi).reset();
		verify(mockServletResponse).setStatus(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void testNextPicture_WhenSchedulerThrowsIOException_ReturnsCorrectErrorResponse() throws IOException {
		when(mockScheduler.nextItem()).thenThrow(new IOException("Device unreachable"));

		MeuralResponse response = controller.nextPicture(mockServletResponse);

		assertNotNull(response);
		assertInstanceOf(MeuralResponse.class, response);
		assertEquals("fail", response.getStatus());
		assertFalse(response.isSuccessful());

		verify(mockApi).reset();
		verify(mockServletResponse).setStatus(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void testIsAsleep_WhenDeviceReachable_ReturnsSuccessfulResponse() throws IOException {
		MeuralStatusResponse mockResponse = new MeuralStatusResponse();
		mockResponse.setStatus("pass");
		mockResponse.setResponse(true);
		when(mockApi.isAsleep()).thenReturn(mockResponse);

		MeuralStatusResponse response = controller.isAsleep(mockServletResponse);

		assertNotNull(response);
		assertInstanceOf(MeuralStatusResponse.class, response);
		assertEquals("pass", response.getStatus());
		assertTrue(response.isSuccessful());
		assertTrue(response.isResponse());

		// Verify no error handling was triggered
		verify(mockApi, never()).reset();
		verify(mockServletResponse, never()).setStatus(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void testIsAsleep_WhenDeviceReturnsFailure_SetsBadRequestStatus() throws IOException {
		MeuralStatusResponse mockResponse = new MeuralStatusResponse();
		mockResponse.setStatus("fail");
		when(mockApi.isAsleep()).thenReturn(mockResponse);

		MeuralStatusResponse response = controller.isAsleep(mockServletResponse);

		assertNotNull(response);
		assertEquals("fail", response.getStatus());
		assertFalse(response.isSuccessful());

		verify(mockServletResponse).setStatus(HttpStatus.BAD_REQUEST.value());
		verify(mockApi, never()).reset(); // No IOException, so no reset
	}

	@Test
	void testErrorResponseCreation_WithValidResponseTypes() {

		assertDoesNotThrow(() -> {
			try {
				when(mockApi.isAsleep()).thenThrow(new IOException("Test"));
				MeuralStatusResponse statusResponse = controller.isAsleep(mockServletResponse);
				assertInstanceOf(MeuralStatusResponse.class, statusResponse);
			} catch (Exception e) {
				reset(mockApi, mockServletResponse);
			}
		});

		assertDoesNotThrow(() -> {
			try {
				when(mockApi.sleep()).thenThrow(new IOException("Test"));
				MeuralStringResponse stringResponse = controller.sleep(mockServletResponse);
				assertInstanceOf(MeuralStringResponse.class, stringResponse);
			} catch (Exception e) {
				reset(mockApi, mockServletResponse);
			}
		});
	}
}

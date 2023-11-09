package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.*;
import com.bigboxer23.meural_control.google.GooglePhotosComponent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** */
@RestController
@Tag(
		name = "Meural Control Service",
		description = "Various APIs available for interacting with the meural and setting" + " content sources to use.")
public class MeuralController {
	private final MeuralComponent api;

	private final SchedulerComponent scheduler;

	private final OpenAIComponent openAIComponent;

	private final GooglePhotosComponent gPhotosAPI;

	private static final Logger logger = LoggerFactory.getLogger(MeuralController.class);

	public MeuralController(
			MeuralComponent api,
			SchedulerComponent scheduler,
			OpenAIComponent openAIComponent,
			GooglePhotosComponent gPhotosAPI) {
		this.api = api;
		this.scheduler = scheduler;
		this.openAIComponent = openAIComponent;
		this.gPhotosAPI = gPhotosAPI;
	}

	private <T extends MeuralResponse> T handleResponse(HttpServletResponse servletResponse, Command<T> command) {
		try {
			MeuralResponse response = command.execute();
			if (!response.isSuccessful()) {
				servletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			}
			return (T) response;
		} catch (IOException theE) {
			logger.warn("handleResponse", theE);
			api.reset();
			servletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			return (T) new MeuralResponse();
		}
	}

	@PostMapping(value = "/displayContentFromUrl", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Change the Meural's display to what's defined by the url.",
			description = "The url could be a remote resource, or it could be a local file.  The resource"
					+ " should have an extension embedded in the link unless it is a PNG file. "
					+ " The file will be added to a Meural playlist defined by the"
					+ " `meural-playlist` property.")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	@Parameters({
		@Parameter(
				name = "url",
				description = "A resource to display on the Meural's display",
				required = true,
				example =
						"https://res.cloudinary.com/dk-find-out/image/upload/q_80,w_1440,f_auto/DCTM_Penguin_UK_DK_AL316928_wsfijk.jpg")
	})
	public MeuralStringResponse displayContentFromUrl(String url, HttpServletResponse servletResponse) {
		logger.warn("change display to: " + url);
		return handleResponse(servletResponse, () -> api.changePicture(new SourceItem(null, new URL(url))));
	}

	@PostMapping(value = "/previewContentFromUrl", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Use Meural's preview functionality to display to what's defined by the url.",
			description = "The url could be a remote resource, or it could be a local file.  The resource"
					+ " should have an extension embedded in the link unless it is a PNG file. "
					+ " The file will be use the Meural device's local postcard endpoint to the"
					+ " Meural (preview functionality) and will stay on the display for the"
					+ " amount of time configured for the preview functionality. This is useful"
					+ " for temporary things like displaying content from a home security"
					+ " camera on detection of movement.")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	@Parameters({
		@Parameter(
				name = "url",
				description = "A resource to temporarily display on the Meural",
				required = true,
				example =
						"https://res.cloudinary.com/dk-find-out/image/upload/q_80,w_1440,f_auto/DCTM_Penguin_UK_DK_AL316928_wsfijk.jpg")
	})
	public MeuralStringResponse previewContentFromUrl(String url, HttpServletResponse servletResponse) {
		logger.warn("previewing: " + url);
		return handleResponse(servletResponse, () -> api.previewItem(new SourceItem(null, new URL(url)), true));
	}

	@GetMapping(value = "/isAsleep", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Check if the Meural is asleep.",
			description = "If the Meural is asleep return true, otherwise false.  \"Response\" field"
					+ " within return value denotes state")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public MeuralStatusResponse isAsleep(HttpServletResponse servletResponse) {
		return handleResponse(servletResponse, api::isAsleep);
	}

	@PostMapping(value = "/sleep", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Put the meural to sleep if it's awake",
			description = "If the Meural is awake, will put it to sleep.")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public MeuralStringResponse sleep(HttpServletResponse servletResponse) {
		return handleResponse(servletResponse, api::sleep);
	}

	@PostMapping(value = "/wakeup", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Wake the Meural if it's asleep",
			description = "If the Meural is sleeping, this will turn it on.")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public MeuralStringResponse wakeup(HttpServletResponse servletResponse) {
		return handleResponse(servletResponse, api::wakeup);
	}

	@PostMapping(value = "/showInfo", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Display info about what's currently displayed",
			description = "Display info about what's currently displayed on the Meural's display")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public MeuralStringResponse showInfo(HttpServletResponse servletResponse) {
		return handleResponse(servletResponse, api::up);
	}

	@PostMapping(value = "/hideInfo", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "If displaying info no the Meural display, hide it",
			description = "If displaying info no the Meural display, hide it")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public MeuralStringResponse hideInfo(HttpServletResponse servletResponse) {
		return handleResponse(servletResponse, api::down);
	}

	@PostMapping(value = "/nextPicture", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Go to next piece of artwork",
			description = "Whatever source is defined in the scheduler, go to the next item from the" + " source")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public MeuralResponse nextPicture(HttpServletResponse servletResponse) {
		return handleResponse(servletResponse, scheduler::nextItem);
	}

	@PostMapping(value = "/prevPicture", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Go to previous piece of artwork",
			description = "Whatever source is defined in the scheduler, go to the previous item from the" + " source")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public MeuralResponse prevPicture(HttpServletResponse servletResponse) {
		return handleResponse(servletResponse, scheduler::prevItem);
	}

	@PostMapping(value = "/updateOpenAIPrompt", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Updates the prompt used to generate the images from OpenAI component",
			description = "This prompt is sent to OpenAI's generator and an AI creates an image based on" + " this")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	@Parameters({
		@Parameter(
				name = "prompt",
				description = "Text prompt for OpenAI to generate an image from",
				required = true,
				example = "A blue unicorn jumping over a red fence while carrying a knight on his"
						+ " back in the style of Andy Warhol")
	})
	public MeuralResponse updateOpenAIPrompt(String prompt, HttpServletResponse servletResponse) {
		openAIComponent.updatePrompt(prompt);
		return handleResponse(servletResponse, scheduler::nextItem);
	}

	@GetMapping(value = "/getOpenAIPrompt", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Gets the prompt used to generate the images from OpenAI component",
			description = "This prompt was last sent to OpenAI's Dall-e 3 for image creation.")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public MeuralStringResponse getOpenAIPrompt(HttpServletResponse servletResponse) {
		return handleResponse(servletResponse, () -> {
			MeuralStringResponse response = new MeuralStringResponse();
			response.setStatus("pass");
			response.setResponse(openAIComponent.getPrompt().orElse(""));
			return response;
		});
	}

	@GetMapping(value = "/getOpenAIInformation", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Get information that's being fed into Dall-e",
			description = "This information was last sent to OpenAI's Dall-e 3 for image creation.")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public ResponseEntity<OpenAIInformation>  getOpenAIInformation() {
		return new ResponseEntity<>(new OpenAIInformation(
				openAIComponent.getPrompt().orElse(""),
				StringUtils.defaultIfBlank(openAIComponent.getQuality(), "hd"),
				StringUtils.defaultIfBlank(openAIComponent.getStyle(), "vivid")), HttpStatus.OK);
	}

	@PostMapping(value = "/changeOpenAIStyle", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Set the associated style used to generate the image")
	@ApiResponses({
			@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
			@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	@Parameters({
			@Parameter(
					name = "style",
					description = "style to generate image with, vivid or natural",
					required = true,
					example = "vivid | natural")
	})
	public ResponseEntity<Void> changeOpenAIStyle(String style) {
		openAIComponent.setStyle(style);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/changeOpenAIQuality", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Set the associated quality used to generate the image")
	@ApiResponses({
			@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
			@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	@Parameters({
			@Parameter(
					name = "quality",
					description = "style to generate image with, hd or standard",
					required = true,
					example = "hd | standard")
	})
	public ResponseEntity<Void> changeOpenAIQuality(String quality) {
		openAIComponent.setQuality(quality);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/changeGooglePhotosAlbum", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Changes the Google Photos album content is displayed from.",
			description = "Must be a valid album name.  Additionally the source is changed to Google"
					+ " Photos automatically when this api is called.")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	@Parameters({
		@Parameter(
				name = "albumTitle",
				description = "title of album in google photos to display from",
				required = true,
				example = "AI Art")
	})
	public MeuralResponse changeGooglePhotosAlbum(String albumTitle, HttpServletResponse servletResponse) {
		gPhotosAPI.changeAlbum(albumTitle);
		return changeSource(0, servletResponse);
	}

	@PostMapping(value = "/changeSource", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Changes the source where new images are fetched from",
			description = "Currently supported sources are from google photos, and from OpenAI Dall-e 3")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	@Parameters({
		@Parameter(
				name = "source",
				description = "ordinal to change backing sources.",
				required = true,
				example = "0=Google Photos Album, 1=OpenAI TextCompletion Dall-e 3, 2=OpenAI"
						+ " ChatGPT-3  Dall-e 3, 3=OpenAI ChatGPT-4 Dall-e 3, 4=James Webb"
						+ " Space Telescope",
				schema =
						@Schema(
								type = "string",
								defaultValue = "0",
								allowableValues = {"0", "1", "2", "3", "4"}))
	})
	public MeuralResponse changeSource(int source, HttpServletResponse servletResponse) {
		scheduler.changeSource(source);
		return handleResponse(servletResponse, scheduler::nextItem);
	}

	@GetMapping(value = "/getCurrentSource", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Get the current source for content",
			description = "Content source, 0 is google photos, 1 is openAI TextCompletion Dall-e 3, 2 is"
					+ " OpenAI ChatGPT-3 Dall-e, 3 is OpenAI ChatGPT-4 Dall-e 3, 4 is James"
					+ " Webb Space Telescope content")
	@ApiResponses({
		@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
		@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")
	})
	public MeuralStringResponse getCurrentSource(HttpServletResponse servletResponse) {
		return handleResponse(servletResponse, scheduler::getSource);
	}
}

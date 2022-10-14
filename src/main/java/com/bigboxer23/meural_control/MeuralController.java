package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.MeuralStatusResponse;
import com.bigboxer23.meural_control.data.MeuralStringResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 */
@RestController
public class MeuralController
{
	private final MeuralAPI api;

	private static final Logger logger = LoggerFactory.getLogger(MeuralController.class);

	public MeuralController(MeuralAPI api)
	{
		this.api = api;
	}

	@PostMapping(value = "/changeDisplayedContent",
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Change the Meural's display to what's defined by the url.",
			description = "The url could be a remote resource, or it could be a local file.  The resource should have an extension" +
					" embedded in the link unless it is a PNG file.  The file will be postcarded to the Meural (preview functionality)" +
					" and will stay on the display for the amount of time configured for the preview functionality.")
	@ApiResponses({@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
			@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")})
	@Parameters({
			@Parameter(name = "url",
					description = "A resource to display on the Meural's display",
					required = true,
					example = "https://res.cloudinary.com/dk-find-out/image/upload/q_80,w_1440,f_auto/DCTM_Penguin_UK_DK_AL316928_wsfijk.jpg"
			)
			})
	public MeuralStringResponse changeDisplayedContent(String url, HttpServletResponse theServletResponse)
	{

		logger.warn("change display");
		try
		{
			MeuralStringResponse aResponse = api.changePicture(new URL(url));
			if (!aResponse.isSuccessful())
			{
				theServletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			}
			return aResponse;
		} catch (IOException theE)
		{
			logger.warn("Error changing picture ", theE);
			api.reset();
			theServletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			return null;
		}
	}

	@GetMapping(value = "/isAsleep",
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Check if the Meural is asleep.",
			description = "If the Meural is asleep return true, otherwise false.  \"Response\" field within return value denotes state")
	@ApiResponses({@ApiResponse(responseCode = HttpURLConnection.HTTP_BAD_REQUEST + "", description = "Bad request"),
			@ApiResponse(responseCode = HttpURLConnection.HTTP_OK + "", description = "success")})
	public MeuralStatusResponse isAsleep(HttpServletResponse theServletResponse)
	{
		try
		{
			MeuralStatusResponse aResponse = api.isAsleep();
			if (!aResponse.isSuccessful())
			{
				theServletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			}
			return aResponse;
		} catch (IOException theE)
		{
			logger.warn("Error changing picture ", theE);
			api.reset();
			theServletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			return null;
		}
	}

	public void nextPicture()
	{
		//TODO:
	}

	public void prevPicture()
	{
		//TODO:
	}
}

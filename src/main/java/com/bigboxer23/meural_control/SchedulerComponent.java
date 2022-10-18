package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.Command;
import com.bigboxer23.meural_control.data.MeuralResponse;
import com.bigboxer23.meural_control.data.MeuralStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Component to schedule changing the Meural's display
 */
@Component
public class SchedulerComponent
{
	private static final Logger logger = LoggerFactory.getLogger(SchedulerComponent.class);

	private final GooglePhotosComponent gPhotosAPI;

	private final MeuralAPI api;

	private final IMeuralImageSource currentSource;

	public SchedulerComponent(GooglePhotosComponent gPhotos, MeuralAPI meuralAPI)
	{
		gPhotosAPI = gPhotos;
		api = meuralAPI;
		currentSource = gPhotos;
	}

	/**
	 * Every hour get a new source file and display on Meural
	 */
	@Scheduled(cron = "${scheduler-time}")
	private void iterateSource() throws IOException
	{
		doAction(gPhotosAPI::nextItem);
	}

	private MeuralResponse doAction(Command<Optional<URL>> command) throws IOException
	{
		MeuralStatusResponse response = api.isAsleep();
		if (response.isResponse())
		{
			logger.info("Meural is asleep, not doing anything");
			return response;
		}
		return command.execute()
				.map(url -> {
			try
			{
				return api.changePicture(url);
			} catch (IOException theE)
			{
				logger.warn("doAction", theE);
				return new MeuralResponse();
			}
		})
				.orElse(new MeuralResponse());
	}

	public void changeSource()
	{
		//TODO:
	}

	public MeuralResponse nextItem() throws IOException
	{
		return doAction(gPhotosAPI::nextItem);
	}

	public MeuralResponse prevItem() throws IOException
	{
		return doAction(gPhotosAPI::prevItem);
	}
}

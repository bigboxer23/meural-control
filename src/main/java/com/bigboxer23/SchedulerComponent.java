package com.bigboxer23;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Component to schedule changing the Meural's display
 */
@Component
public class SchedulerComponent
{
	private static final Logger logger = LoggerFactory.getLogger(SchedulerComponent.class);

	private GooglePhotosComponent gPhotosAPI;

	private MeuralAPI api;

	private IMeuralImageSource currentSource;

	public SchedulerComponent(GooglePhotosComponent gPhotos, MeuralAPI meuralAPI)
	{
		gPhotosAPI = gPhotos;
		api = meuralAPI;
		currentSource = gPhotos;
	}

	/**
	 * Every hour get a new source file and display on Meural
	 */
	@Scheduled(fixedDelay = 3600000)
	private void iterateSource()
	{
		currentSource.nextItem().ifPresent(item -> {
			try
			{
				api.changePicture(item);
			}
			catch (IOException theE)
			{
				logger.warn("iterateSource", theE);
			}
		});
	}

	public void changeSource()
	{
		//TODO:
	}
}

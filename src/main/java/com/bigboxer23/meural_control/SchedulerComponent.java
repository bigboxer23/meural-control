package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.*;
import com.bigboxer23.meural_control.util.FilePersistentIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Component to schedule changing the Meural's display
 */
@Component
public class SchedulerComponent
{
	private static final Logger logger = LoggerFactory.getLogger(SchedulerComponent.class);

	private final GooglePhotosComponent gPhotosAPI;

	private final OpenAIComponent openAIAPI;

	private final MeuralComponent api;

	private IMeuralImageSource currentSource;

	private FilePersistentIndex sourceStorage = new FilePersistentIndex("source");

	public SchedulerComponent(GooglePhotosComponent gPhotos, MeuralComponent meuralComponent, OpenAIComponent openAIComponent)
	{
		gPhotosAPI = gPhotos;
		openAIAPI = openAIComponent;
		api = meuralComponent;
		changeSource(sourceStorage.get());
	}

	/**
	 * Every hour get a new source file and display on Meural
	 */
	@Scheduled(cron = "${scheduler-time}")
	private void iterateSource() throws IOException
	{
		doAction(currentSource::nextItem);
	}

	private MeuralResponse doAction(Command<Optional<SourceItem>> command) throws IOException
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

	public void changeSource(int sourceOrdinal)
	{
		switch (sourceOrdinal)
		{
			case 0:
			default:
				currentSource = gPhotosAPI;
				break;
			case 1:
				currentSource = openAIAPI;
				break;
		}
		sourceStorage.set(sourceOrdinal);
	}

	public MeuralStringResponse getSource()
	{
		MeuralStringResponse response = new MeuralStringResponse();
		response.setStatus("pass");
		response.setResponse(sourceStorage.get() + "");
		return response;
	}

	public MeuralResponse nextItem() throws IOException
	{
		return doAction(currentSource::nextItem);
	}

	public MeuralResponse prevItem() throws IOException
	{
		return doAction(currentSource::prevItem);
	}
}

package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.Command;
import com.bigboxer23.meural_control.data.MeuralResponse;
import com.bigboxer23.meural_control.data.MeuralStatusResponse;
import com.bigboxer23.meural_control.data.SourceItem;
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

	private final MeuralAPI api;

	private IMeuralImageSource currentSource;

	public SchedulerComponent(GooglePhotosComponent gPhotos, MeuralAPI meuralAPI, OpenAIComponent openAIComponent)
	{
		gPhotosAPI = gPhotos;
		openAIAPI = openAIComponent;
		api = meuralAPI;
		currentSource = gPhotos;
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

package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.*;
import com.bigboxer23.meural_control.google.GooglePhotosComponent;
import com.bigboxer23.meural_control.jwst.JWSTComponent;
import com.bigboxer23.utils.file.FilePersistentIndex;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Component to schedule changing the Meural's display */
@Component
public class SchedulerComponent {
	private static final Logger logger = LoggerFactory.getLogger(SchedulerComponent.class);

	private final GooglePhotosComponent gPhotosAPI;

	private final OpenAIComponent openAIAPI;

	private final JWSTComponent jwstAPI;

	private final MeuralComponent api;

	private IMeuralImageSource currentSource;

	private FilePersistentIndex sourceStorage = new FilePersistentIndex("meuralSource");

	public SchedulerComponent(
			GooglePhotosComponent gPhotos,
			MeuralComponent meuralComponent,
			OpenAIComponent openAIComponent,
			JWSTComponent jwstComponent) {
		gPhotosAPI = gPhotos;
		openAIAPI = openAIComponent;
		jwstAPI = jwstComponent;
		api = meuralComponent;
		changeSource(sourceStorage.get());
	}

	/** Every hour get a new source file and display on Meural */
	@Scheduled(cron = "${scheduler-time}")
	private void iterateSource() throws IOException {
		doAction(currentSource::nextItem);
	}

	private MeuralResponse doAction(Command<Optional<SourceItem>> command) throws IOException {
		MeuralStatusResponse response = api.isAsleep();
		if (response.isResponse()) {
			logger.info("Meural is asleep, not doing anything");
			return response;
		}
		return command.execute()
				.map(url -> {
					try {
						url.setCleanupTempFile(false);
						// preview first b/c next, more permanent step takes (10-15s)
						api.previewItem(url, false);
						url.setCleanupTempFile(true);
						return api.changePicture(url);
					} catch (IOException theE) {
						logger.warn("doAction", theE);
						return new MeuralResponse();
					}
				})
				.orElse(new MeuralResponse());
	}

	public void changeSource(int sourceOrdinal) {
		switch (sourceOrdinal) {
			case 0:
			default:
				currentSource = gPhotosAPI;
				break;
			case 1:
			case 2:
			case 3:
				currentSource = openAIAPI;
				openAIAPI.setMode(sourceOrdinal);
				break;
			case 4:
				currentSource = jwstAPI;
				break;
		}
		sourceStorage.set(sourceOrdinal);
	}

	public MeuralStringResponse getSource() {
		MeuralStringResponse response = new MeuralStringResponse();
		response.setStatus("pass");
		response.setResponse(sourceStorage.get() + "");
		return response;
	}

	public MeuralResponse nextItem() throws IOException {
		return doAction(currentSource::nextItem);
	}

	public MeuralResponse prevItem() throws IOException {
		return doAction(currentSource::prevItem);
	}
}

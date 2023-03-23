package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.*;
import com.bigboxer23.meural_control.google.GooglePhotosComponent;
import com.bigboxer23.utils.http.OkHttpUtil;
import com.bigboxer23.utils.http.RequestBuilderCallback;
import com.squareup.moshi.Moshi;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** */
@Component
public class MeuralComponent {
	private static final Logger logger = LoggerFactory.getLogger(MeuralComponent.class);

	private final Moshi moshi = new Moshi.Builder().build();

	@Value("${meural-api}")
	private String apiUrl;

	@Value("${meural-account}")
	private String username;

	@Value("${meural-password}")
	private String password;

	@Value("${meural-playlist}")
	private String playlistName;

	@Value("${meural-orientation}")
	private String meuralOrientation;

	private String token;

	private Device meuralDevice;

	private final GooglePhotosComponent gPhotos;

	private final ImageTransformComponent transformComponent;

	public MeuralComponent(GooglePhotosComponent gPhotos, ImageTransformComponent transform) {
		this.gPhotos = gPhotos;
		transformComponent = transform;
	}

	private String getToken() {
		if (token == null) {
			logger.info("fetching service meural token");
			try (Response response = OkHttpUtil.postSynchronous(
					apiUrl + "authenticate",
					new FormBody.Builder()
							.add("username", username)
							.add("password", password)
							.build(),
					null)) {
				if (!response.isSuccessful()) {
					throw new IOException(response.message());
				}
				token = moshi.adapter(Token.class)
						.fromJson(response.body().string())
						.getToken();
			} catch (IOException e) {
				logger.error("getToken", e);
			}
		}
		return token;
	}

	private RequestBuilderCallback getAuthCallback() {
		return builder -> builder.addHeader("Authorization", "Token " + getToken());
	}

	private Device getDevice() throws IOException {
		if (meuralDevice != null) {
			return meuralDevice;
		}
		logger.info("fetching device info from meural service");
		try (Response response =
				OkHttpUtil.getSynchronous(apiUrl + "user/devices?count=10&page=1", getAuthCallback())) {
			if (!response.isSuccessful()) {
				logger.warn("Cannot get device " + response.code());
				throw new IOException("Cannot get device " + response.code());
			}
			String body = response.body().string();
			Devices devices = moshi.adapter(Devices.class).fromJson(body);
			if (devices == null || devices.getData() == null || devices.getData().length == 0) {
				logger.warn("cannot get device from body " + body);
				throw new IOException("cannot get device from body " + body);
			}
			meuralDevice = devices.getData()[0];
			return meuralDevice;
		}
	}

	private MeuralStringResponse addItemToPlaylistAndDisplay(SourceItem sourceItem) throws IOException {
		logger.info("starting add new file to playlist \"" + sourceItem.getName() + "\"");
		MeuralItem item = uploadItemToMeural(sourceItem);
		MeuralPlaylist playlist = getOrCreatePlaylist();
		// Check if we already had this item in the playlist. If we did, no need to do anything
		if (Arrays.stream(playlist.getItemIds()).noneMatch(id -> id == Integer.parseInt(item.getId()))) {
			addItemToPlaylist(playlist.getId(), item.getId());
			deleteItemsFromPlaylist(playlist);
			addPlaylistToDevice(getDevice().getId(), playlist.getId());
		}
		MeuralStringResponse response = new MeuralStringResponse();
		response.setStatus("pass");
		return response;
	}

	private void addPlaylistToDevice(String deviceId, String playlistId) throws IOException {
		logger.info("Adding playlist to Meural " + deviceId + ":" + playlistId);
		try (Response response = OkHttpUtil.postSynchronous(
				apiUrl + "devices/" + deviceId + "/galleries/" + playlistId, null, getAuthCallback())) {
			if (!response.isSuccessful()) {
				throw new IOException(
						"Cannot add to playlist to device" + response.body().string());
			}
		}
	}

	private void deleteItemsFromPlaylist(MeuralPlaylist playlist) throws IOException {
		logger.info("deleting items from playlist " + playlist.getId());
		for (Integer item : playlist.getItemIds()) {
			deleteItem(item);
		}
	}

	private void deleteItem(Integer itemId) throws IOException {
		logger.info("deleting item: " + itemId);
		try (Response response = OkHttpUtil.deleteSynchronous(apiUrl + "items/" + itemId, getAuthCallback())) {
			if (!response.isSuccessful()) {
				throw new IOException(
						"Cannot add to playlist " + response.body().string());
			}
		}
	}

	private void addItemToPlaylist(String playlistId, String itemId) throws IOException {
		logger.info("adding item to playlist " + playlistId + ":" + itemId);
		try (Response response = OkHttpUtil.postSynchronous(
				apiUrl + "galleries/" + playlistId + "/items/" + itemId, null, getAuthCallback())) {
			if (!response.isSuccessful()) {
				throw new IOException(
						"Cannot add to playlist " + response.body().string());
			}
		}
	}

	private String getMeuralName(SourceItem sourceItem) {
		if (sourceItem.getName().length() <= 512) {
			return sourceItem.getName();
		}
		return StringUtils.truncate(sourceItem.getName(), 512) + "." + FilenameUtils.getExtension(sourceItem.getName());
	}

	private MeuralItem uploadItemToMeural(SourceItem sourceItem) throws IOException {
		sourceItem.setTempFile(transformComponent.transformItem(sourceItem.getTempFile()));
		logger.info("uploading file to Meural service \"" + sourceItem.getName() + "\"");
		try (Response response = OkHttpUtil.postSynchronous(
				apiUrl + "items",
				new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart(
								"image",
								getMeuralName(sourceItem),
								RequestBody.create(sourceItem.getTempFile(), getMediaType(sourceItem.getTempFile())))
						.build(),
				getAuthCallback())) {
			String body = response.body().string();
			MeuralItemResponse itemResponse =
					moshi.adapter(MeuralItemResponse.class).fromJson(body);
			if (itemResponse == null || itemResponse.getData() == null) {
				logger.warn("cannot get item from body " + body);
				throw new IOException("cannot get item from body " + body);
			}
			return itemResponse.getData();
		}
	}

	private MeuralPlaylist getOrCreatePlaylist() throws IOException {
		logger.info("get playlist info for \"" + playlistName + "\"");
		try (Response response =
				OkHttpUtil.getSynchronous(apiUrl + "user/galleries?count=10&page=1", getAuthCallback())) {
			String body = response.body().string();
			MeuralPlaylists meuralPlaylists =
					moshi.adapter(MeuralPlaylists.class).fromJson(body);
			if (meuralPlaylists == null || meuralPlaylists.getData() == null || meuralPlaylists.getData().length == 0) {
				logger.warn("cannot get playlists from body " + body);
				throw new IOException("cannot get playlists from body " + body);
			}
			MeuralPlaylist playlist = Arrays.stream(meuralPlaylists.getData())
					.filter(theMeuralPlaylist -> playlistName.equalsIgnoreCase(theMeuralPlaylist.getName()))
					.findAny()
					.orElseGet(() -> {
						try {
							return createPlaylist();
						} catch (IOException e) {
							logger.warn("orElseGet", e);
							return null;
						}
					});
			if (playlist == null) {
				throw new IOException("cannot get playlist"); // Can't throw any exception from orElseGet directly
			}
			return playlist;
		}
	}

	private MeuralPlaylist createPlaylist() throws IOException {
		logger.info("Creating playlist for " + playlistName);
		try (Response response = OkHttpUtil.postSynchronous(
				apiUrl + "galleries",
				new FormBody.Builder()
						.add("name", playlistName)
						.add("orientation", meuralOrientation)
						.build(),
				getAuthCallback())) {
			String body = response.body().string();
			MeuralPlaylistResponse playlistResponse =
					moshi.adapter(MeuralPlaylistResponse.class).fromJson(body);
			if (playlistResponse == null || playlistResponse.getData() == null) {
				logger.warn("cannot create playlist from body " + body);
				throw new IOException("cannot create playlist from body " + body);
			}
			return playlistResponse.getData();
		}
	}

	private String getDeviceIP() throws IOException {
		return getDevice().getFrameStatus().getLocalIp();
	}

	private MediaType getMediaType(File file) {
		switch (FilenameUtils.getExtension(file.getName())) {
			case "jpg":
			case "jpeg":
				return MediaType.parse("image/jpeg");
			case "gif":
				return MediaType.parse("image/gif");
			case "png":
			default:
				return MediaType.parse("image/png");
		}
	}

	/** Cause a re-fetch of all meural device information on next request */
	public void reset() {
		token = null;
		meuralDevice = null;
	}

	private MeuralStringResponse executeAfterFetchCommand(SourceItem item, Command<MeuralStringResponse> command)
			throws IOException {
		try {
			return command.execute();
		} finally {
			if (item.isCleanupTempFile()) {
				item.getTempFile().delete();
			}
		}
	}

	private MeuralStringResponse fetchItem(SourceItem item, Command<MeuralStringResponse> command) throws IOException {
		// If temp file is set and exists, don't fetch it again.
		if (item.getTempFile() != null && item.getTempFile().exists()) {
			logger.info("item exists, not re-downloading \"" + item.getName() + "\"");
			return executeAfterFetchCommand(item, command);
		}
		logger.info("downloading item for \"" + item.getName() + "\"");
		String extension = FilenameUtils.getExtension(
				item.getName() != null
						? item.getName()
						: (item.getUrl().toString().contains("?")
								? item.getUrl()
										.toString()
										.substring(0, item.getUrl().toString().lastIndexOf("?"))
								: item.getUrl().toString()));
		Path temp = Files.createTempFile("", "." + extension);
		HttpURLConnection conn = (HttpURLConnection) item.getUrl().openConnection();
		conn.setRequestProperty(
				"User-Agent",
				"Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203"
						+ " Firefox/3.6.13 (.NET CLR 3.5.30729)");
		try (InputStream stream = conn.getInputStream()) {
			FileUtils.copyInputStreamToFile(stream, temp.toFile());
			item.setTempFile(temp.toFile());
			if (item.getAlbumToSaveTo() != null && item.getAlbumToSaveTo().length() > 0) {
				gPhotos.uploadItemToAlbum(item);
			}
			return executeAfterFetchCommand(item, command);
		}
	}

	public MeuralStringResponse previewItem(SourceItem item, boolean transform) throws IOException {
		return fetchItem(item, () -> changePictureWithPreview(item, transform));
	}

	public MeuralStringResponse changePicture(SourceItem item) throws IOException {
		return fetchItem(item, () -> addItemToPlaylistAndDisplay(item));
	}

	/**
	 * This is a much quicker solution than using meural services, but previewing doesn't seem to
	 * consistently hold for my newer meural device
	 *
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public MeuralStringResponse changePictureWithPreview(SourceItem item, boolean transform) throws IOException {
		File file = item.getTempFile();
		if (transform) {
			file = transformComponent.transformPreviewItem(file);
		}
		logger.info("previewing directly on meural \"" + item.getName() + "\"");
		try (Response response = OkHttpUtil.postSynchronous(
				getDeviceURL() + "/remote/postcard",
				new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("photo", "1", RequestBody.create(file, getMediaType(file)))
						.build(),
				null)) {
			MeuralStringResponse meuralResponse = moshi.adapter(MeuralStringResponse.class)
					.fromJson(response.body().string());
			if (!meuralResponse.isSuccessful()) {
				logger.warn("failure to change " + meuralResponse.getResponse());
			}
			return meuralResponse;
		}
	}

	/**
	 * Is the meural presently asleep?
	 *
	 * @return
	 * @throws IOException
	 */
	public MeuralStatusResponse isAsleep() throws IOException {
		return doRequest("/remote/control_check/sleep", MeuralStatusResponse.class);
	}

	public MeuralStringResponse wakeup() throws IOException {
		return doRequest("/remote/control_command/resume", MeuralStringResponse.class);
	}

	public MeuralStringResponse sleep() throws IOException {
		return doRequest("/remote/control_command/suspend", MeuralStringResponse.class);
	}

	public MeuralStringResponse up() throws IOException {
		return doRequest("/remote/control_command/set_key/up", MeuralStringResponse.class);
	}

	public MeuralStringResponse down() throws IOException {
		return doRequest("/remote/control_command/set_key/down", MeuralStringResponse.class);
	}

	private <T extends MeuralResponse> T doRequest(String command, Class<T> theResult) throws IOException {
		try (Response response = OkHttpUtil.getSynchronous(getDeviceURL() + command, null)) {
			return moshi.adapter(theResult).fromJson(response.body().string());
		}
	}

	private String getDeviceURL() throws IOException {
		return "http://" + getDeviceIP();
	}
}

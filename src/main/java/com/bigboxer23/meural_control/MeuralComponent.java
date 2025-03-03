package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.*;
import com.bigboxer23.meural_control.google.GooglePhotosComponent;
import com.bigboxer23.utils.command.Command;
import com.bigboxer23.utils.command.RetryingCommand;
import com.bigboxer23.utils.http.OkHttpUtil;
import com.bigboxer23.utils.http.RequestBuilderCallback;
import com.squareup.moshi.JsonEncodingException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** */
@Slf4j
@Component
public class MeuralComponent {
	private static final String apiUrl = "https://api.meural.com/v0/";

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

	protected String getToken() {
		if (token == null) {
			log.info("fetching service meural token");
			try (Response response = OkHttpUtil.postSynchronous(
					apiUrl + "authenticate",
					new FormBody.Builder()
							.add("username", username)
							.add("password", password)
							.build(),
					null)) {
				token = OkHttpUtil.getNonEmptyBody(response, Token.class).getToken();
			} catch (IOException e) {
				log.error("getToken", e);
			}
		}
		return token;
	}

	private RequestBuilderCallback getAuthCallback() {
		return builder -> builder.addHeader("Authorization", "Token " + getToken());
	}

	protected Device getDevice() throws IOException {
		if (meuralDevice != null) {
			return meuralDevice;
		}
		log.info("fetching device info from meural service");
		try (Response response =
				OkHttpUtil.getSynchronous(apiUrl + "user/devices?count=10&page=1", getAuthCallback())) {
			Devices devices = OkHttpUtil.getNonEmptyBody(response, Devices.class);
			if (devices == null || devices.getData() == null || devices.getData().length == 0) {
				log.warn("cannot get device from body ");
				throw new IOException("cannot get device from body ");
			}
			meuralDevice = devices.getData()[0];
			return meuralDevice;
		}
	}

	private MeuralStringResponse addItemToPlaylistAndDisplay(SourceItem sourceItem) throws IOException {
		log.info("starting add new file to playlist: \"" + sourceItem.getName() + "\"");
		MeuralItem item = uploadItemToMeural(sourceItem);
		MeuralPlaylist playlist = getOrCreatePlaylist();
		// Check if we already had this item in the playlist. If we did, no need to do anything
		if (Arrays.stream(playlist.getItemIds()).noneMatch(id -> id == Integer.parseInt(item.getId()))) {
			addItemToPlaylist(playlist.getId(), item.getId());
			deleteItemsFromPlaylist(playlist);
			addPlaylistToDevice(getDevice().getId(), playlist.getId());
		}
		log.info("completed adding new file to playlist: \"" + sourceItem.getName() + "\"");
		MeuralStringResponse response = new MeuralStringResponse();
		response.setStatus("pass");
		return response;
	}

	private void addPlaylistToDevice(String deviceId, String playlistId) throws IOException {
		log.info("Adding playlist to Meural " + deviceId + ":" + playlistId);
		RetryingCommand.builder()
				.identifier("add playlist" + playlistId)
				.failureCommand(resetCommand())
				.buildAndExecute(() -> {
					try (Response response = OkHttpUtil.postSynchronous(
							apiUrl + "devices/" + deviceId + "/galleries/" + playlistId, null, getAuthCallback())) {
						if (!response.isSuccessful()) {
							throw new IOException("Cannot add to playlist to device"
									+ response.body().string());
						}
					}
					return null;
				});
	}

	private void deleteItemsFromPlaylist(MeuralPlaylist playlist) throws IOException {
		log.info("deleting items from playlist " + playlist.getId());
		for (Integer item : playlist.getItemIds()) {
			deleteItem(item);
		}
	}

	private void deleteItem(Integer itemId) throws IOException {
		log.info("deleting item: " + itemId);
		RetryingCommand.builder()
				.identifier("delete item" + itemId)
				.failureCommand(resetCommand())
				.buildAndExecute(() -> {
					try (Response response =
							OkHttpUtil.deleteSynchronous(apiUrl + "items/" + itemId, getAuthCallback())) {
						if (!response.isSuccessful()) {
							throw new IOException(
									"Cannot add to playlist " + response.body().string());
						}
					}
					return null;
				});
	}

	private void addItemToPlaylist(String playlistId, String itemId) throws IOException {
		log.info("adding item to playlist " + playlistId + ":" + itemId);
		RetryingCommand.builder()
				.identifier("add item" + itemId)
				.failureCommand(resetCommand())
				.buildAndExecute(() -> {
					try (Response response = OkHttpUtil.postSynchronous(
							apiUrl + "galleries/" + playlistId + "/items/" + itemId, null, getAuthCallback())) {
						if (!response.isSuccessful()) {
							throw new IOException(
									"Cannot add to playlist " + response.body().string());
						}
					}
					return null;
				});
	}

	private String getMeuralName(SourceItem sourceItem) {
		if (sourceItem.getName().length() <= 512) {
			return sourceItem.getName();
		}
		return StringUtils.truncate(sourceItem.getName(), 512) + "." + FilenameUtils.getExtension(sourceItem.getName());
	}

	private MeuralItem uploadItemToMeural(SourceItem sourceItem) throws IOException {
		sourceItem.setTempFile(transformComponent.transformItem(sourceItem.getTempFile()));
		log.info("uploading file to Meural service \"" + sourceItem.getName() + "\"");
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

			try {
				MeuralItemResponse itemResponse = OkHttpUtil.getNonEmptyBody(response, MeuralItemResponse.class);
				if (itemResponse == null || itemResponse.getData() == null) {
					log.warn("cannot get item from body ");
					reset();
					throw new IOException("cannot get item from body ");
				}
				return itemResponse.getData();
			} catch (JsonEncodingException e) {
				log.warn("uploadItemToMeural exception: ", e);
				throw e;
			}
		}
	}

	protected MeuralPlaylist getOrCreatePlaylist() throws IOException {
		log.info("get playlist info for \"" + playlistName + "\"");
		return RetryingCommand.builder()
				.identifier("getOrCreatePlaylist ")
				.failureCommand(resetCommand())
				.buildAndExecute(() -> {
					try (Response response =
							OkHttpUtil.getSynchronous(apiUrl + "user/galleries?count=10&page=1", getAuthCallback())) {
						MeuralPlaylists meuralPlaylists = OkHttpUtil.getNonEmptyBody(response, MeuralPlaylists.class);
						if (meuralPlaylists == null
								|| meuralPlaylists.getData() == null
								|| meuralPlaylists.getData().length == 0) {
							log.warn("cannot get playlists from body ");
							throw new IOException("cannot get playlists from body ");
						}
						MeuralPlaylist playlist = Arrays.stream(meuralPlaylists.getData())
								.filter(theMeuralPlaylist -> playlistName.equalsIgnoreCase(theMeuralPlaylist.getName()))
								.findAny()
								.orElseGet(() -> {
									try {
										return createPlaylist(playlistName);
									} catch (IOException e) {
										log.warn("orElseGet", e);
										return null;
									}
								});
						if (playlist == null) {
							throw new IOException("cannot get playlist"); // Can't throw any exception
							// from
							// orElseGet directly
						}
						return playlist;
					}
				});
	}

	protected MeuralPlaylist createPlaylist(String name) throws IOException {
		log.info("Creating playlist for " + name);
		return RetryingCommand.builder()
				.identifier("createPlaylist " + name)
				.failureCommand(resetCommand())
				.buildAndExecute(() -> {
					try (Response response = OkHttpUtil.postSynchronous(
							apiUrl + "galleries",
							new FormBody.Builder()
									.add("name", name)
									.add("orientation", meuralOrientation)
									.build(),
							getAuthCallback())) {
						MeuralPlaylistResponse playlistResponse =
								OkHttpUtil.getNonEmptyBody(response, MeuralPlaylistResponse.class);
						if (playlistResponse == null || playlistResponse.getData() == null) {
							log.warn("cannot create playlist from body ");
							throw new IOException("cannot create playlist from body ");
						}
						return playlistResponse.getData();
					}
				});
	}

	protected void deletePlaylist(String playlistId) throws IOException {
		log.info("deleting playlist for " + playlistId);
		RetryingCommand.builder()
				.identifier("delete playlist" + playlistId)
				.failureCommand(resetCommand())
				.buildAndExecute(() -> {
					try (Response response =
							OkHttpUtil.deleteSynchronous(apiUrl + "galleries/" + playlistId, getAuthCallback())) {
						if (!response.isSuccessful()) {
							log.warn("cannot delete playlist "
									+ playlistId
									+ ", body: "
									+ response.body().string());
							throw new IOException("cannot create playlist " + playlistId);
						}
					}
					return null;
				});
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
		log.warn("resetting api");
		token = null;
		meuralDevice = null;
	}

	private Command<Void> resetCommand() {
		return () -> {
			reset();
			return null;
		};
	}

	private MeuralStringResponse executeAfterFetchCommand(SourceItem item, Command<MeuralStringResponse> command)
			throws IOException {
		try {
			return command.execute();
		} finally {
			if (item.isCleanupTempFile()) {
				if (item.getAlbumToSaveTo() != null && item.getAlbumToSaveTo().length() > 0) {
					gPhotos.uploadItemToAlbum(item);
				}
				log.info("removing temp file: \"" + item.getName() + "\"");
				item.getTempFile().delete();
			}
		}
	}

	public MeuralStringResponse fetchItem(SourceItem item, Command<MeuralStringResponse> command) throws IOException {
		// If temp file is set and exists, don't fetch it again.
		if (item.getTempFile() != null && item.getTempFile().exists()) {
			log.info("item exists, not re-downloading \"" + item.getName() + "\"");
			return executeAfterFetchCommand(item, command);
		}
		log.info("downloading item for \"" + item.getName() + "\"");
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
		File tmpFile = item.getTempFile();
		if (transform) {
			tmpFile = transformComponent.transformPreviewItem(tmpFile);
		}
		File file = tmpFile;
		log.info("previewing directly on meural \"" + item.getName() + "\"");
		return RetryingCommand.builder()
				.identifier("changePictureWithPreview")
				.failureCommand(resetCommand())
				.buildAndExecute(() -> {
					try (Response response = OkHttpUtil.postSynchronous(
							getDeviceURL() + "/remote/postcard",
							new MultipartBody.Builder()
									.setType(MultipartBody.FORM)
									.addFormDataPart("photo", "1", RequestBody.create(file, getMediaType(file)))
									.build(),
							null)) {
						return OkHttpUtil.getNonEmptyBody(response, MeuralStringResponse.class);
					}
				});
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

	private <T extends MeuralResponse> T doRequest(String command, Class<T> clazz) throws IOException {
		try (Response response = OkHttpUtil.getSynchronous(getDeviceURL() + command, null)) {
			return OkHttpUtil.getNonEmptyBody(response, clazz);
		}
	}

	private String getDeviceURL() throws IOException {
		return "http://" + getDeviceIP();
	}
}

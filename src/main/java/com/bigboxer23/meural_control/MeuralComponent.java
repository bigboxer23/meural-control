package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.*;
import com.squareup.moshi.Moshi;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 */
@Component
public class MeuralComponent
{
	private static final Logger logger = LoggerFactory.getLogger(MeuralComponent.class);

	private final OkHttpClient client = new OkHttpClient();

	private final Moshi moshi = new Moshi.Builder().build();

	@Value("${meural-api}")
	private String apiUrl;

	@Value("${meural-account}")
	private String username;

	@Value("${meural-password}")
	private String password;

	private String token;

	private String meuralIP;

	private GooglePhotosComponent gPhotos;

	public MeuralComponent(GooglePhotosComponent gPhotos)
	{
		this.gPhotos = gPhotos;
	}

	private String getToken() throws IOException
	{
		if (token == null)
		{
			RequestBody formBody = new FormBody
					.Builder()
					.add("username", username)
					.add("password", password)
					.build();
			Request request = new Request
					.Builder()
					.url(apiUrl + "authenticate")
					.post(formBody)
					.build();
			try (Response response = client.newCall(request).execute())
			{
				if (response.isSuccessful())
				{
					token = moshi
							.adapter(Token.class)
							.fromJson(response.body().string()).getToken();
				}
				logger.warn("authenticate response: " + response.code());
			}
		}
		return token;
	}

	private Device getDevice() throws IOException
	{
		Request request = new Request.Builder()
				.url(apiUrl + "user/devices?count=10&page=1")
				.addHeader("Authorization", "Token " + getToken())
				.build();
		try (Response response = client.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				logger.warn("Cannot get device " + response.code());
				throw new IOException("Cannot get device " + response.code());
			}
			String body = response.body().string();
			Devices devices = moshi
					.adapter(Devices.class)
					.fromJson(body);
			if (devices == null
					|| devices.getData() == null
					|| devices.getData().length == 0)
			{
				logger.warn("cannot get device from body " + body);
				throw new IOException("cannot get device from body " + body);
			}
			return devices.getData()[0];
		}
	}

	private String getDeviceIP() throws IOException
	{
		if (meuralIP == null)
		{
			meuralIP = getDevice().getFrameStatus().getLocalIp();
		}
		return meuralIP;
	}

	private MediaType getMediaType(File file)
	{
		switch (FilenameUtils.getExtension(file.getName()))
		{
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

	/**
	 * Cause a re-fetch of all meural device information on next request
	 */
	public void reset()
	{
		token = null;
		meuralIP = null;
	}

	public MeuralStringResponse changePicture(SourceItem item) throws IOException
	{
		String extension = FilenameUtils.getExtension(item.getName() != null ? item.getName()
				: FilenameUtils.getExtension(item.getUrl().toString()));
		Path temp = Files.createTempFile("", "." + extension);
		HttpURLConnection conn = (HttpURLConnection)item.getUrl().openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");
		try (InputStream stream = conn.getInputStream()) {
			FileUtils.copyInputStreamToFile(stream, temp.toFile());
			if (item.getAlbumToSaveTo() != null && item.getAlbumToSaveTo().length() > 0)
			{
				item.setTempFile(temp.toFile());
				gPhotos.uploadItemToAlbum(item);
			}
			return changePicture(temp.toFile());
		} finally
		{
			temp.toFile().delete();
		}
	}

	public MeuralStringResponse changePicture(File file) throws IOException
	{
		logger.info("changing picture " + file.getAbsolutePath());
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("photo", "1",
						RequestBody.create(file, getMediaType(file)))
				.build();
		Request request = new Request
				.Builder()
				.url(getDeviceURL() + "/remote/postcard")
				.post(requestBody)
				.build();
		try (Response response = client.newCall(request).execute())
		{
			MeuralStringResponse aResponse =  moshi
					.adapter(MeuralStringResponse.class)
					.fromJson(response.body().string());
			if (!aResponse.isSuccessful())
			{
				logger.warn("failure to change " + aResponse.getResponse());
			}
			return aResponse;
		}
	}

	/**
	 * Is the meural presently asleep?
	 *
	 * @return
	 * @throws IOException
	 */
	public MeuralStatusResponse isAsleep() throws IOException
	{
		return doRequest("/remote/control_check/sleep", MeuralStatusResponse.class);
	}

	public MeuralStringResponse wakeup() throws IOException
	{
		return doRequest("/remote/control_command/resume", MeuralStringResponse.class);
	}

	public MeuralStringResponse sleep() throws IOException
	{
		return doRequest("/remote/control_command/suspend", MeuralStringResponse.class);
	}

	private <T extends MeuralResponse> T doRequest(String command, Class<T> theResult) throws IOException
	{
		Request request = new Request
				.Builder()
				.url(getDeviceURL() + command)
				.get()
				.build();
		try (Response response = client.newCall(request).execute())
		{
			return moshi
					.adapter(theResult)
					.fromJson(response.body().string());
		}
	}

	private String getDeviceURL() throws IOException
	{
		return "http://" + getDeviceIP();
	}
}
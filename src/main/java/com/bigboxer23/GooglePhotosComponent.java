package com.bigboxer23;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.collect.ImmutableList;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient;
import com.google.photos.types.proto.MediaItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Component to interface with Google Photos so content can be pushed direct (and live from cloud) to the Meural
 * device without needing to depend on Meural services.
 */
@Component
public class GooglePhotosComponent implements IMeuralImageSource
{
	private static final Logger logger = LoggerFactory.getLogger(GooglePhotosComponent.class);

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	private static final List<String> REQUIRED_SCOPES =
			ImmutableList.of(
					"https://www.googleapis.com/auth/photoslibrary.readonly",
					"https://www.googleapis.com/auth/photoslibrary.appendonly");

	@Value("${gPhotos-albumTitle}")
	private String albumTitle;

	private String albumId;

	private CredentialsProvider credProvider;

	private int currentItem = -1;

	/**
	 * Iterate through an album's content and
	 */
	@Override
	public Optional<URL> nextItem()
	{
		try
		{
			PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder().setCredentialsProvider(getCredentialProvider()).build();
			try (PhotosLibraryClient photosLibraryClient = PhotosLibraryClient.initialize(settings))
			{
				InternalPhotosLibraryClient.SearchMediaItemsPagedResponse aResults = photosLibraryClient.searchMediaItems(findAlbumId(photosLibraryClient));
				Iterator<MediaItem> items = aResults.iterateAll().iterator();
				for (int ai = 0; items.hasNext(); ai++)
				{
					MediaItem item = items.next();
					if (ai == currentItem)
					{
						currentItem++;
						logger.warn("returning item " + currentItem + " from album " + albumTitle);
						return Optional.of(new URL(item.getBaseUrl()));
					}
				}
				currentItem = 0;
			}
		} catch (IOException | GeneralSecurityException theE)
		{
			logger.warn("nextItem:", theE);
		}
		return Optional.empty();
	}

	private String findAlbumId(PhotosLibraryClient client)
	{
		if (albumId == null)
		{
			client.listAlbums().iterateAll().forEach(album -> {
				if (album.getTitle().equals(albumTitle))
				{
					albumId = album.getId();
				}
			});
		}
		return albumId;
	}

	private CredentialsProvider getCredentialProvider() throws IOException, GeneralSecurityException
	{
		if (credProvider == null)
		{
			logger.info("Getting google creds");
			InputStream aCredStream = Credentials.class.getResourceAsStream("/credentials.json");
			GoogleClientSecrets aClientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(aCredStream));
			GoogleAuthorizationCodeFlow aFlow = new GoogleAuthorizationCodeFlow.Builder(
					GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, aClientSecrets, REQUIRED_SCOPES)
					.setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
					.setAccessType("offline")
					.build();
			logger.info("Starting local server receiver");
			Credential credential = new AuthorizationCodeInstalledApp(aFlow, new LocalServerReceiver.Builder().setPort(8890).build()).authorize("user");
			credProvider = FixedCredentialsProvider.create(UserCredentials.newBuilder()
					.setClientId(aClientSecrets.getDetails().getClientId())
					.setClientSecret(aClientSecrets.getDetails().getClientSecret())
					.setRefreshToken(credential.getRefreshToken())
					.build());
		}
		return credProvider;
	}
}
